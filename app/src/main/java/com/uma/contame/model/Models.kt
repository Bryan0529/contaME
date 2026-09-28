package com.uma.contame.model

import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME
}

data class TransactionCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String
)

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

data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadlineTimestamp: Long? = null,
    val colorHex: String = "#3B82F6",
    val iconName: String = "flag"
) {
    val progress: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentAmount >= targetAmount
}

data class MonthlyBudget(
    val monthYearKey: String, // e.g. "2026-09"
    val budgetLimit: Double = 1500.0,
    val alertThresholdPercent: Int = 80, // e.g., 80% triggers alert
    val notificationsEnabled: Boolean = true
)

data class CategoryBreakdown(
    val categoryName: String,
    val categoryIcon: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthlyStat(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

object DefaultCategories {
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

    val incomeCategories = listOf(
        TransactionCategory("inc_salary", "Salario", TransactionType.INCOME, "payments", "#10B981"),
        TransactionCategory("inc_business", "Negocio", TransactionType.INCOME, "storefront", "#059669"),
        TransactionCategory("inc_invest", "Inversiones", TransactionType.INCOME, "trending_up", "#0D9488"),
        TransactionCategory("inc_freelance", "Freelance", TransactionType.INCOME, "work", "#14B8A6"),
        TransactionCategory("inc_gift", "Regalos / Extras", TransactionType.INCOME, "card_giftcard", "#34D399"),
        TransactionCategory("inc_other", "Otros Ingresos", TransactionType.INCOME, "attach_money", "#6EE7B7")
    )

    fun getCategoryById(id: String): TransactionCategory {
        return (expenseCategories + incomeCategories).find { it.id == id }
            ?: TransactionCategory(id, "General", TransactionType.EXPENSE, "category", "#94A3B8")
    }
}
