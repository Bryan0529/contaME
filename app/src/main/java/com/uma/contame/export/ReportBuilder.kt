package com.uma.contame.export

import android.content.Context
import com.uma.contame.model.TransactionType
import com.uma.contame.viewmodel.ContaMeUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Construye los reportes de contaME a partir del estado de la UI ([ContaMeUiState]).
 *
 * Solo **lee** datos que la app ya tiene; no modifica Room, Firestore ni el ViewModel.
 * Produce dos formatos: un texto bonito para WhatsApp ([buildWhatsAppText]) y un libro de
 * Excel ([buildExcelFile]).
 */
object ReportBuilder {

    private val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    private fun money(v: Double) = String.format(Locale.US, "\$%,.2f", v)

    /** Estado del semáforo financiero según el consumo del presupuesto. */
    private fun trafficLight(s: ContaMeUiState): Pair<String, String> = when {
        s.isBudgetExceeded -> "🔴" to "Rojo (presupuesto excedido)"
        s.isBudgetWarning -> "🟡" to "Amarillo (cerca del límite)"
        else -> "🟢" to "Verde (vas bien)"
    }

    /**
     * Texto del resumen mensual con el formato de WhatsApp (`*negrita*`, emojis, líneas cortas).
     *
     * @param state Estado actual de la app.
     * @return Mensaje listo para compartir.
     */
    fun buildWhatsAppText(state: ContaMeUiState): String {
        val (icon, _) = trafficLight(state)
        val pct = (state.budgetUsagePercent * 100).toInt()
        val sb = StringBuilder()
        sb.appendLine("📊 *contaME – ${state.selectedMonthLabel}*")
        sb.appendLine("━━━━━━━━━━━━━━")
        sb.appendLine("💰 Ingresos: *${money(state.monthlyIncome)}*")
        sb.appendLine("💸 Gastos: *${money(state.monthlyExpense)}*")
        sb.appendLine("$icon Balance del mes: *${money(state.monthlyIncome - state.monthlyExpense)}*")
        sb.appendLine("🎯 Presupuesto: ${money(state.monthlyBudget.budgetLimit)} (usado $pct%)")

        val topExpenses = state.expenseCategoriesBreakdown.sortedByDescending { it.totalAmount }.take(3)
        if (topExpenses.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("*Top gastos*")
            topExpenses.forEachIndexed { i, c -> sb.appendLine("${i + 1}. ${c.categoryName}: ${money(c.totalAmount)}") }
        }

        if (state.savingsGoals.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("*Metas de ahorro*")
            state.savingsGoals.take(3).forEach { g ->
                val done = if (g.isCompleted) " ✅" else ""
                sb.appendLine("• ${g.title}: ${(g.progress * 100).toInt()}%$done")
            }
        }

        val last = state.transactions.sortedByDescending { it.timestamp }.take(3)
        if (last.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("*Últimos movimientos*")
            last.forEach { t ->
                val sign = if (t.type == TransactionType.INCOME) "+" else "-"
                sb.appendLine("• ${t.title}: $sign${money(t.amount)}")
            }
        }
        sb.appendLine()
        sb.append("_Generado con contaME_")
        return sb.toString()
    }

    /** Texto corto para el cuerpo de un correo. */
    fun buildEmailBody(state: ContaMeUiState): String =
        "Hola,\n\nAdjunto mi reporte de finanzas de ${state.selectedMonthLabel} generado con contaME.\n\n" +
                "Ingresos: ${money(state.monthlyIncome)}\n" +
                "Gastos: ${money(state.monthlyExpense)}\n" +
                "Balance: ${money(state.monthlyIncome - state.monthlyExpense)}\n\n" +
                "El archivo de Excel incluye el detalle de movimientos, historial mensual, metas y registro de acciones."

    /**
     * Genera el archivo de Excel con 5 hojas: Resumen, Movimientos, Historial mensual, Metas y
     * Registro de acciones.
     *
     * @param context Contexto (se usa la carpeta de caché de la app como destino).
     * @param state Estado actual de la app.
     * @return Archivo `.xlsx` listo para compartir.
     */
    fun buildExcelFile(context: Context, state: ContaMeUiState): File {
        val sheets = listOf(summarySheet(state), transactionsSheet(state), historySheet(state), goalsSheet(state), actionsSheet(context, state))
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "contaME_reporte_${state.selectedMonthKey}.xlsx")
        XlsxWriter.write(file, sheets)
        return file
    }

    private fun summarySheet(s: ContaMeUiState): XlsxSheet {
        val sh = XlsxSheet("Resumen")
        sh.columnWidths = listOf(30.0, 28.0)
        sh.addRow(XlsxCell("contaME – Reporte de ${s.selectedMonthLabel}", XlsxSheet.STYLE_BOLD))
        sh.addValues("Generado", dateTime.format(Date()))
        s.userProfile?.let { sh.addValues("Usuario", it.email ?: it.displayName ?: "") }
        sh.addBlank()
        sh.addHeader("Concepto", "Valor")
        sh.addRow(XlsxCell("Ingresos del mes"), XlsxCell(s.monthlyIncome, XlsxSheet.STYLE_MONEY))
        sh.addRow(XlsxCell("Gastos del mes"), XlsxCell(s.monthlyExpense, XlsxSheet.STYLE_MONEY))
        sh.addRow(XlsxCell("Balance del mes", XlsxSheet.STYLE_BOLD), XlsxCell(s.monthlyIncome - s.monthlyExpense, XlsxSheet.STYLE_MONEY))
        sh.addRow(XlsxCell("Saldo acumulado"), XlsxCell(s.currentBalance, XlsxSheet.STYLE_MONEY))
        sh.addRow(XlsxCell("Límite de presupuesto"), XlsxCell(s.monthlyBudget.budgetLimit, XlsxSheet.STYLE_MONEY))
        sh.addRow(XlsxCell("Presupuesto usado"), XlsxCell(s.budgetUsagePercent.toDouble(), XlsxSheet.STYLE_PERCENT))
        sh.addValues("Semáforo financiero", trafficLight(s).second)
        sh.addBlank()
        sh.addHeader("Gastos por categoría", "Total")
        s.expenseCategoriesBreakdown.sortedByDescending { it.totalAmount }.forEach {
            sh.addRow(XlsxCell(it.categoryName), XlsxCell(it.totalAmount, XlsxSheet.STYLE_MONEY))
        }
        return sh
    }

    private fun transactionsSheet(s: ContaMeUiState): XlsxSheet {
        val sh = XlsxSheet("Movimientos")
        sh.columnWidths = listOf(18.0, 12.0, 20.0, 30.0, 14.0, 40.0)
        sh.addHeader("Fecha", "Tipo", "Categoría", "Título", "Monto", "Notas")
        s.transactions.sortedByDescending { it.timestamp }.forEach { t ->
            sh.addRow(
                XlsxCell(dateTime.format(Date(t.timestamp))),
                XlsxCell(if (t.type == TransactionType.INCOME) "Ingreso" else "Gasto"),
                XlsxCell(t.categoryName),
                XlsxCell(t.title),
                XlsxCell(t.amount, XlsxSheet.STYLE_MONEY),
                XlsxCell(t.notes)
            )
        }
        return sh
    }

    /** Historial mensual completo (todos los meses con movimientos), calculado desde los movimientos guardados. */
    private fun historySheet(s: ContaMeUiState): XlsxSheet {
        val sh = XlsxSheet("Historial mensual")
        sh.columnWidths = listOf(14.0, 16.0, 16.0, 16.0, 14.0)
        sh.addHeader("Mes", "Ingresos", "Gastos", "Balance", "Movimientos")
        s.transactions.groupBy { monthKey.format(Date(it.timestamp)) }.toSortedMap().forEach { (key, items) ->
            val inc = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            sh.addRow(
                XlsxCell(key),
                XlsxCell(inc, XlsxSheet.STYLE_MONEY),
                XlsxCell(exp, XlsxSheet.STYLE_MONEY),
                XlsxCell(inc - exp, XlsxSheet.STYLE_MONEY),
                XlsxCell(items.size)
            )
        }
        return sh
    }

    private fun goalsSheet(s: ContaMeUiState): XlsxSheet {
        val sh = XlsxSheet("Metas")
        sh.columnWidths = listOf(28.0, 16.0, 16.0, 12.0, 16.0, 14.0)
        sh.addHeader("Meta", "Objetivo", "Ahorrado", "Progreso", "Fecha límite", "Estado")
        val day = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        s.savingsGoals.forEach { g ->
            sh.addRow(
                XlsxCell(g.title),
                XlsxCell(g.targetAmount, XlsxSheet.STYLE_MONEY),
                XlsxCell(g.currentAmount, XlsxSheet.STYLE_MONEY),
                XlsxCell(g.progress.toDouble(), XlsxSheet.STYLE_PERCENT),
                XlsxCell(g.deadlineTimestamp?.let { day.format(Date(it)) } ?: "Sin fecha"),
                XlsxCell(if (g.isCompleted) "Completada" else "En curso")
            )
        }
        return sh
    }

    /**
     * Registro de acciones: usa el [ActivityLog] si tiene datos; si está vacío (por ejemplo, si
     * aún no se conectó al ViewModel) muestra los movimientos como acciones "Movimiento registrado".
     */
    private fun actionsSheet(context: Context, s: ContaMeUiState): XlsxSheet {
        val sh = XlsxSheet("Registro de acciones")
        sh.columnWidths = listOf(18.0, 26.0, 36.0, 14.0)
        sh.addHeader("Fecha", "Acción", "Detalle", "Monto")
        val log = ActivityLog.readAll(context)
        if (log.isNotEmpty()) {
            log.forEach { e ->
                sh.addRow(
                    XlsxCell(dateTime.format(Date(e.timestamp))),
                    XlsxCell(e.action),
                    XlsxCell(e.detail),
                    XlsxCell(e.amount, XlsxSheet.STYLE_MONEY)
                )
            }
        } else {
            s.transactions.sortedByDescending { it.timestamp }.forEach { t ->
                val kind = if (t.type == TransactionType.INCOME) "Ingreso" else "Gasto"
                sh.addRow(
                    XlsxCell(dateTime.format(Date(t.timestamp))),
                    XlsxCell("Movimiento registrado ($kind)"),
                    XlsxCell("${t.categoryName}: ${t.title}"),
                    XlsxCell(t.amount, XlsxSheet.STYLE_MONEY)
                )
            }
        }
        return sh
    }
}