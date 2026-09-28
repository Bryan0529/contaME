package com.uma.contame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE or INCOME
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val timestamp: Long,
    val notes: String
) {
    fun toDomain(): TransactionItem = TransactionItem(
        id = id,
        title = title,
        amount = amount,
        type = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        timestamp = timestamp,
        notes = notes
    )

    companion object {
        fun fromDomain(domain: TransactionItem, userId: String = "local"): TransactionEntity = TransactionEntity(
            id = domain.id,
            userId = userId,
            title = domain.title,
            amount = domain.amount,
            type = domain.type.name,
            categoryId = domain.categoryId,
            categoryName = domain.categoryName,
            categoryIcon = domain.categoryIcon,
            categoryColor = domain.categoryColor,
            timestamp = domain.timestamp,
            notes = domain.notes
        )
    }
}

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadlineTimestamp: Long?,
    val colorHex: String,
    val iconName: String
) {
    fun toDomain(): SavingsGoal = SavingsGoal(
        id = id,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        deadlineTimestamp = deadlineTimestamp,
        colorHex = colorHex,
        iconName = iconName
    )

    companion object {
        fun fromDomain(domain: SavingsGoal, userId: String = "local"): SavingsGoalEntity = SavingsGoalEntity(
            id = domain.id,
            userId = userId,
            title = domain.title,
            targetAmount = domain.targetAmount,
            currentAmount = domain.currentAmount,
            deadlineTimestamp = domain.deadlineTimestamp,
            colorHex = domain.colorHex,
            iconName = domain.iconName
        )
    }
}

@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey val id: String, // "$userId-$monthYearKey"
    val userId: String = "local",
    val monthYearKey: String,
    val budgetLimit: Double,
    val alertThresholdPercent: Int,
    val notificationsEnabled: Boolean
) {
    fun toDomain(): MonthlyBudget = MonthlyBudget(
        monthYearKey = monthYearKey,
        budgetLimit = budgetLimit,
        alertThresholdPercent = alertThresholdPercent,
        notificationsEnabled = notificationsEnabled
    )

    companion object {
        fun fromDomain(domain: MonthlyBudget, userId: String = "local"): BudgetEntity = BudgetEntity(
            id = "$userId-${domain.monthYearKey}",
            userId = userId,
            monthYearKey = domain.monthYearKey,
            budgetLimit = domain.budgetLimit,
            alertThresholdPercent = domain.alertThresholdPercent,
            notificationsEnabled = domain.notificationsEnabled
        )
    }
}
