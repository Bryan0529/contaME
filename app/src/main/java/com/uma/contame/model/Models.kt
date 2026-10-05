package com.uma.contame.model

import java.util.UUID

/**
 * Enumeración que define el tipo de movimiento financiero en la aplicación.
 */
enum class TransactionType {
    /** Movimiento de salida de dinero (gastos). */
    EXPENSE,
    /** Movimiento de entrada de dinero (ingresos). */
    INCOME
}

/**
 * Modelo que representa una categoría de transacción.
 *
 * @property id Identificador único de la categoría (ej: "exp_food").
 * @property name Nombre legible de la categoría (ej: "Alimentación").
 * @property type Tipo de movimiento ([TransactionType.EXPENSE] o [TransactionType.INCOME]).
 * @property iconName Nombre del icono de Material Icons asociado.
 * @property colorHex Código de color hexadecimal asignado a la categoría.
 */
data class TransactionCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String
)

/**
 * Modelo de dominio que representa un registro individual de movimiento financiero.
 *
 * @property id ID único de la transacción (generado por defecto mediante [UUID]).
 * @property title Título o concepto del gasto o ingreso.
 * @property amount Valor monetario del movimiento.
 * @property type Tipo de movimiento (gasto o ingreso).
 * @property categoryId ID de la categoría asociada.
 * @property categoryName Nombre visible de la categoría.
 * @property categoryIcon Nombre del icono.
 * @property categoryColor Color hexadecimal asociado.
 * @property timestamp Fecha y hora del registro en milisegundos.
 * @property notes Notas o detalles opcionales sobre la transacción.
 */
data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Modelo de dominio que representa una meta de ahorro definida por el usuario.
 *
 * @property id Identificador único de la meta.
 * @property title Nombre u objetivo de la meta (ej: "Fondo de emergencia").
 * @property targetAmount Monto objetivo a alcanzar.
 * @property currentAmount Monto ahorrado hasta el momento.
 * @property deadlineTimestamp Fecha límite en milisegundos (opcional).
 * @property colorHex Color identificador en formato hexadecimal.
 * @property iconName Nombre del icono asignado.
 */
data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadlineTimestamp: Long? = null,
    val colorHex: String = "#3B82F6",
    val iconName: String = "flag"
) {
    /**
     * Calcula el porcentaje de avance hacia la meta (valor entre 0.0f y 1.0f).
     */
    val progress: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    /**
     * Indica si la meta de ahorro ya ha sido alcanzada o superada.
     */
    val isCompleted: Boolean
        get() = currentAmount >= targetAmount
}

/**
 * Modelo que representa la configuración del presupuesto mensual de gastos.
 *
 * @property monthYearKey Clave del mes y año correspondiente (ej: "2026-03").
 * @property budgetLimit Límite de gasto máximo definido para el mes.
 * @property alertThresholdPercent Porcentaje de consumo a partir del cual se emitirá una alerta (ej: 80%).
 * @property notificationsEnabled Estado que determina si las notificaciones locales están activadas.
 */
data class MonthlyBudget(
    val monthYearKey: String, // e.g. "2026-09"
    val budgetLimit: Double = 1500.0,
    val alertThresholdPercent: Int = 80, // e.g., 80% triggers alert
    val notificationsEnabled: Boolean = true
)

/**
 * Modelo utilizado para generar gráficos de desglose por categoría.
 *
 * @property categoryName Nombre de la categoría.
 * @property categoryIcon Icono de la categoría.
 * @property colorHex Color representativo en gráficos.
 * @property totalAmount Sumatoria de los montos gastados/ingresados en la categoría.
 * @property percentage Porcentaje relativo del total general.
 */
data class CategoryBreakdown(
    val categoryName: String,
    val categoryIcon: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float
)

/**
 * Modelo para representar estadísticas comparativas mensuales (Ingresos vs. Gastos).
 *
 * @property monthLabel Etiqueta del mes (ej: "Ene", "Feb").
 * @property income Total acumulado de ingresos.
 * @property expense Total acumulado de gastos.
 */
data class MonthlyStat(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

/**
 * Objeto contenedor con las categorías predefinidas de la aplicación para gastos e ingresos.
 */
object DefaultCategories {
    /** Categorías predeterminadas de gastos. */
    val expenseCategories = listOf(
        TransactionCategory("exp_food", "Alimentación", TransactionType.EXPENSE, "restaurant", "#EF4444"),
        TransactionCategory("exp_transport", "Transporte", TransactionType.EXPENSE, "directions_car", "#F97316"),
        TransactionCategory("exp_housing", "Vivienda", TransactionType.EXPENSE, "home", "#8B5CF6"),
        TransactionCategory("exp_services", "Servicios", TransactionType.EXPENSE, "bolt", "#EAB308"),
        TransactionCategory("exp_entertainment", "Ocio y Salidas", TransactionType.EXPENSE, "sports_esports", "#EC4899"),
        TransactionCategory("exp_health", "Salud y Cuidado", TransactionType.EXPENSE, "favorite", "#06B6D4"),
        TransactionCategory("exp_education", "Educación", TransactionType.EXPENSE, "school", "#3B82F6"),
        TransactionCategory("exp_shopping", "Compras", TransactionType.EXPENSE, "shopping_cart", "#F43F5E"),
        TransactionCategory("exp_other", "Otros Gastos", TransactionType.EXPENSE, "more_horiz", "#64748B")
    )

    /** Categorías predeterminadas de ingresos. */
    val incomeCategories = listOf(
        TransactionCategory("inc_salary", "Salario", TransactionType.INCOME, "payments", "#10B981"),
        TransactionCategory("inc_business", "Negocio", TransactionType.INCOME, "storefront", "#059669"),
        TransactionCategory("inc_invest", "Inversiones", TransactionType.INCOME, "trending_up", "#0D9488"),
        TransactionCategory("inc_freelance", "Freelance", TransactionType.INCOME, "work", "#14B8A6"),
        TransactionCategory("inc_gift", "Regalos / Extras", TransactionType.INCOME, "card_giftcard", "#34D399"),
        TransactionCategory("inc_other", "Otros Ingresos", TransactionType.INCOME, "attach_money", "#6EE7B7")
    )

    /**
     * Busca y retorna una categoría por su ID. Retorna una categoría "General" por defecto si no se encuentra.
     */
    fun getCategoryById(id: String): TransactionCategory {
        return (expenseCategories + incomeCategories).find { it.id == id }
            ?: TransactionCategory(id, "General", TransactionType.EXPENSE, "category", "#94A3B8")
    }
}
