package com.uma.contame.export

import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Da formato al resumen para WhatsApp. Es Kotlin puro (sin Android) para poder probarlo fácilmente.
 *
 * WhatsApp solo admite `*negrita*`, `_cursiva_`, `~tachado~` y bloques de texto monoespaciado
 * (tres comillas invertidas), por eso se combinan con emojis y barras de color para lograr un
 * mensaje ordenado y llamativo.
 */
object WhatsAppFormatter {

    private const val LINE = "━━━━━━━━━━━━━━━━"
    private val NUMBERS = listOf("1️⃣", "2️⃣", "3️⃣")

    private fun money(v: Double): String =
        (if (v < 0) "-" else "") + String.format(Locale.US, "\$%,.2f", abs(v))

    /**
     * Barra de progreso de 10 cuadros. Los cuadros llenos usan [filled] (🟩, 🟨 o 🟥) y los vacíos ⬜.
     */
    fun bar(fraction: Float, filled: String): String {
        val n = (fraction.coerceIn(0f, 1f) * 10).roundToInt()
        return filled.repeat(n) + "⬜".repeat(10 - n)
    }

    /**
     * Construye el mensaje completo.
     *
     * @param monthLabel Mes del reporte (ej: "Noviembre 2026").
     * @param income Ingresos del mes.
     * @param expense Gastos del mes.
     * @param budgetLimit Límite del presupuesto mensual (0 o menos = no mostrar la sección).
     * @param usage Fracción consumida del presupuesto (1.0 = 100 %, puede ser mayor).
     * @param exceeded `true` si se superó el presupuesto.
     * @param warning `true` si se está cerca del límite.
     * @param topExpenses Categorías de gasto más altas (nombre y total), ya ordenadas; se usan las 3 primeras.
     * @param goals Metas de ahorro: nombre, progreso (0..1) y si está completada; se usan las 3 primeras.
     * @param lastTransactions Últimos movimientos, ya ordenados; se usan los 3 primeros.
     */
    fun build(
        monthLabel: String,
        income: Double,
        expense: Double,
        budgetLimit: Double,
        usage: Float,
        exceeded: Boolean,
        warning: Boolean,
        topExpenses: List<Pair<String, Double>>,
        goals: List<Triple<String, Float, Boolean>>,
        lastTransactions: List<TransactionItem>
    ): String {
        val balance = income - expense
        val sb = StringBuilder()
        sb.appendLine("✨ *contaME* ✨")
        sb.appendLine("_Resumen de ${monthLabel}_")
        sb.appendLine(LINE)
        sb.appendLine()
        sb.appendLine("```")
        sb.appendLine(String.format(Locale.US, "%-9s %12s", "Ingresos", money(income)))
        sb.appendLine(String.format(Locale.US, "%-9s %12s", "Gastos", money(expense)))
        sb.appendLine(String.format(Locale.US, "%-9s %12s", "Balance", money(balance)))
        sb.appendLine("```")
        sb.appendLine(if (balance >= 0) "💚 *¡Vas en positivo este mes!*" else "🔴 *Este mes gastaste más de lo que ingresó*")

        if (budgetLimit > 0) {
            val light = if (exceeded) "🟥" else if (warning) "🟨" else "🟩"
            sb.appendLine()
            sb.appendLine("🎯 *Presupuesto* (${money(budgetLimit)})")
            sb.appendLine("${bar(usage, light)} *${(usage * 100).roundToInt()}%*")
            val left = budgetLimit - expense
            sb.appendLine(
                when {
                    exceeded -> "⚠️ Excedido por ${money(-left)}"
                    warning -> "🟡 Cerca del límite · te quedan ${money(left)}"
                    else -> "✅ Vas bien · te quedan ${money(left)}"
                }
            )
        }

        if (topExpenses.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🛒 *Donde más gastaste*")
            topExpenses.take(3).forEachIndexed { i, (name, total) -> sb.appendLine("${NUMBERS[i]} $name · ${money(total)}") }
        }

        if (goals.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🐷 *Metas de ahorro*")
            goals.take(3).forEach { (name, progress, done) ->
                sb.appendLine("▫️ $name${if (done) " ✅" else ""}")
                sb.appendLine("${bar(progress, "🟩")} ${(progress * 100).roundToInt()}%")
            }
        }

        if (lastTransactions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🧾 *Últimos movimientos*")
            lastTransactions.take(3).forEach { t ->
                val income = t.type == TransactionType.INCOME
                sb.appendLine("${if (income) "🟢" else "🔴"} ${t.title} · ${if (income) "+" else "-"}${money(t.amount)}")
            }
        }

        sb.appendLine()
        sb.appendLine(LINE)
        sb.append("_Generado con contaME_ 💚")
        return sb.toString()
    }
}
