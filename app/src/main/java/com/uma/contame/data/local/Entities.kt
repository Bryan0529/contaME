package com.uma.contame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType

/**
 * Entidad de Room para la tabla de transacciones de la base de datos local.
 *
 * @property id Identificador único de la transacción.
 * @property userId Identificador del usuario propietario de la transacción (permite soporte multiusuario).
 * @property title Título o descripción corta del movimiento.
 * @property amount Monto económico de la transacción.
 * @property type Tipo de movimiento: "INCOME" (Ingreso) o "EXPENSE" (Gasto).
 * @property categoryId ID de la categoría asociada.
 * @property categoryName Nombre legible de la categoría.
 * @property categoryIcon Icono de la categoría.
 * @property categoryColor Código de color Hexadecimal de la categoría.
 * @property timestamp Fecha y hora de la transacción en milisegundos de época.
 * @property notes Notas o detalles adicionales sobre la transacción.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE o INCOME
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val timestamp: Long,
    val notes: String
) {
    /**
     * Convierte esta entidad de Room al modelo de dominio [TransactionItem].
     */
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
        /**
         * Crea una entidad de Room [TransactionEntity] a partir del modelo de dominio [TransactionItem].
         *
         * @param domain Objeto de dominio a convertir.
         * @param userId Identificador del usuario al que pertenece la transacción.
         */
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

/**
 * Entidad de Room para la tabla de metas de ahorro en la base de datos local.
 *
 * @property id Identificador único de la meta.
 * @property userId Identificador del usuario propietario.
 * @property title Nombre o meta a lograr (ej: "Vacaciones", "Fondo de emergencia").
 * @property targetAmount Monto objetivo deseado.
 * @property currentAmount Monto acumulado actualmente.
 * @property deadlineTimestamp Fecha límite opcional en milisegundos.
 * @property colorHex Color identificador en formato hexadecimal.
 * @property iconName Nombre del icono asignado.
 */
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
    /**
     * Convierte esta entidad al modelo de dominio [SavingsGoal].
     */
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
        /**
         * Transforma un modelo de dominio [SavingsGoal] en su entidad local [SavingsGoalEntity].
         */
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

/**
 * Entidad de Room para la tabla de presupuestos mensuales.
 *
 * @property id Identificador compuesto por "$userId-$monthYearKey" para garantizar unicidad por usuario y mes.
 * @property userId ID del usuario.
 * @property monthYearKey Clave del mes/año en formato "yyyy-MM" (ej: "2025-03").
 * @property budgetLimit Límite presupuestado máximo para gastos en el mes.
 * @property alertThresholdPercent Porcentaje del límite a partir del cual se genera una alerta (ej: 80%).
 * @property notificationsEnabled Estado de activación de las alertas/notificaciones.
 */
@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey val id: String, // "$userId-$monthYearKey"
    val userId: String = "local",
    val monthYearKey: String,
    val budgetLimit: Double,
    val alertThresholdPercent: Int,
    val notificationsEnabled: Boolean
) {
    /**
     * Convierte esta entidad al modelo de dominio [MonthlyBudget].
     */
    fun toDomain(): MonthlyBudget = MonthlyBudget(
        monthYearKey = monthYearKey,
        budgetLimit = budgetLimit,
        alertThresholdPercent = alertThresholdPercent,
        notificationsEnabled = notificationsEnabled
    )

    companion object {
        /**
         * Transforma el modelo [MonthlyBudget] a la entidad [BudgetEntity].
         */
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
