package com.uma.contame.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.uma.contame.viewmodel.ContaMeUiState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

/**
 * Dibuja una tarjeta de resumen mensual (imagen PNG vertical de 1080 × 1350 px, con los colores de
 * contaME) para compartirla por WhatsApp como foto.
 *
 * Usa solo `android.graphics` (sin librerías externas) y solo **lee** el estado de la app.
 */
object ShareImageCard {

    private const val W = 1080
    private const val H = 1350

    private const val BG_TOP = 0xFF0B1220.toInt()
    private const val BG_BOTTOM = 0xFF16233A.toInt()
    private const val CARD = 0xFF1B2A41.toInt()
    private const val GREEN = 0xFF10B981.toInt()
    private const val RED = 0xFFEF4444.toInt()
    private const val YELLOW = 0xFFF59E0B.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MUTED = 0xFF94A3B8.toInt()

    private fun money(v: Double): String =
        (if (v < 0) "-" else "") + String.format(Locale.US, "\$%,.2f", abs(v))

    /**
     * Genera la imagen del mes seleccionado.
     *
     * @param context Contexto (se usa la carpeta de caché de la app).
     * @param state Estado actual de la app.
     * @return Archivo PNG listo para compartir.
     */
    fun render(context: Context, state: ContaMeUiState): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fondo con degradado
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null

        fun rect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
            p.color = color
            c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
        }

        fun text(s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, right: Boolean = false, center: Boolean = false) {
            p.color = color
            p.textSize = size
            p.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            p.textAlign = if (right) Paint.Align.RIGHT else if (center) Paint.Align.CENTER else Paint.Align.LEFT
            c.drawText(s, x, y, p)
        }

        fun fit(s: String, maxWidth: Float, size: Float): String {
            p.textSize = size
            p.typeface = Typeface.DEFAULT
            var t = s
            while (t.length > 1 && p.measureText(t) > maxWidth) t = t.dropLast(1)
            return if (t.length < s.length) t.trimEnd() + "…" else t
        }

        val balance = state.monthlyIncome - state.monthlyExpense
        val usage = state.budgetUsagePercent
        val statusColor = if (state.isBudgetExceeded) RED else if (state.isBudgetWarning) YELLOW else GREEN

        // Encabezado
        text("conta", 70f, 150f, 80f, WHITE, bold = true)
        p.textSize = 80f; p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val contaWidth = p.measureText("conta")
        text("ME", 70f + contaWidth, 150f, 80f, GREEN, bold = true)
        text("Resumen de ${state.selectedMonthLabel}", 70f, 210f, 40f, MUTED)

        // Balance del mes
        text("Balance del mes", 70f, 330f, 38f, MUTED)
        text(money(balance), 70f, 450f, 128f, if (balance >= 0) GREEN else RED, bold = true)

        // Ingresos y gastos
        rect(60f, 520f, 520f, 700f, 36f, CARD)
        text("Ingresos", 100f, 580f, 34f, MUTED)
        text(money(state.monthlyIncome), 100f, 655f, 56f, GREEN, bold = true)
        rect(560f, 520f, 1020f, 700f, 36f, CARD)
        text("Gastos", 600f, 580f, 34f, MUTED)
        text(money(state.monthlyExpense), 600f, 655f, 56f, RED, bold = true)

        // Presupuesto
        val limit = state.monthlyBudget.budgetLimit
        if (limit > 0) {
            text("Presupuesto", 70f, 790f, 44f, WHITE, bold = true)
            text("${(usage * 100).toInt()}%", 1010f, 790f, 44f, statusColor, bold = true, right = true)
            rect(60f, 820f, 1020f, 856f, 18f, CARD)
            val fillWidth = (960f * usage.coerceIn(0f, 1f)).coerceAtLeast(if (usage > 0f) 36f else 0f)
            if (fillWidth > 0f) rect(60f, 820f, 60f + fillWidth, 856f, 18f, statusColor)
            val left = limit - state.monthlyExpense
            text(
                if (left >= 0) "Gastado ${money(state.monthlyExpense)} de ${money(limit)} · quedan ${money(left)}"
                else "Excedido por ${money(-left)} (límite ${money(limit)})",
                70f, 905f, 34f, MUTED
            )
        }

        // Donde más gastaste
        val top = state.expenseCategoriesBreakdown.sortedByDescending { it.totalAmount }.take(3)
        if (top.isNotEmpty()) {
            text("Donde más gastaste", 70f, 1010f, 44f, WHITE, bold = true)
            val maxTotal = top.first().totalAmount.coerceAtLeast(1.0)
            top.forEachIndexed { i, cat ->
                val y = 1075f + i * 95f
                text(fit(cat.categoryName, 600f, 38f), 70f, y, 38f, WHITE)
                text(money(cat.totalAmount), 1010f, y, 38f, MUTED, bold = true, right = true)
                rect(70f, y + 18f, 1010f, y + 30f, 6f, CARD)
                rect(70f, y + 18f, 70f + (940f * (cat.totalAmount / maxTotal)).toFloat().coerceAtLeast(12f), y + 30f, 6f, GREEN)
            }
        } else {
            text("Sin gastos registrados este mes", 70f, 1040f, 40f, MUTED)
        }

        // Pie
        text("Generado con contaME", W / 2f, 1310f, 30f, MUTED, center = true)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "contaME_resumen_${state.selectedMonthKey}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
