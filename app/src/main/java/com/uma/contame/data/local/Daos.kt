package com.uma.contame.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la gestión de la tabla de transacciones en Room.
 */
@Dao
interface TransactionDao {
    /**
     * Obtiene el listado de transacciones de un usuario específico ordenadas por fecha descendente.
     * Retorna un [Flow] que se actualiza reactivamente ante cualquier cambio en la base de datos.
     */
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    /**
     * Obtiene una captura puntual de todas las transacciones de un usuario.
     */
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(userId: String): List<TransactionEntity>

    /**
     * Busca una transacción específica por su ID y el usuario al que pertenece.
     */
    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun getTransactionById(id: String, userId: String): TransactionEntity?

    /**
     * Inserta o reemplaza una transacción existente en caso de conflicto de ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    /**
     * Inserta o reemplaza un listado masivo de transacciones.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /**
     * Elimina una transacción específica de la base de datos local.
     */
    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    /**
     * Borra todas las transacciones asociadas a un usuario particular.
     */
    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearUserTransactions(userId: String)
}

/**
 * Objeto de Acceso a Datos (DAO) para la gestión de las metas de ahorro en Room.
 */
@Dao
interface SavingsGoalDao {
    /**
     * Obtiene todas las metas de ahorro asociadas a un usuario en formato [Flow] reactivo.
     */
    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    fun getAllGoals(userId: String): Flow<List<SavingsGoalEntity>>

    /**
     * Inserta o reemplaza una meta de ahorro.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: SavingsGoalEntity)

    /**
     * Inserta o reemplaza un listado masivo de metas de ahorro.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    /**
     * Elimina una meta de ahorro por su ID y usuario.
     */
    @Query("DELETE FROM savings_goals WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    /**
     * Incrementa el saldo acumulado en una meta de ahorro específica.
     */
    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :addedAmount WHERE id = :id AND userId = :userId")
    suspend fun addFunds(id: String, userId: String, addedAmount: Double)
}

/**
 * Objeto de Acceso a Datos (DAO) para la gestión de los presupuestos mensuales en Room.
 */
@Dao
interface BudgetDao {
    /**
     * Obtiene el presupuesto mensual correspondiente a un mes/año y usuario específicos.
     */
    @Query("SELECT * FROM monthly_budgets WHERE monthYearKey = :key AND userId = :userId LIMIT 1")
    fun getBudgetForMonth(key: String, userId: String): Flow<BudgetEntity?>

    /**
     * Inserta o actualiza un registro de presupuesto mensual.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)
}

/**
 * Base de datos principal de Room para la aplicación `contaME`.
 *
 * Incluye las tablas de transacciones, metas de ahorro y presupuestos mensuales.
 */
@Database(
    entities = [TransactionEntity::class, SavingsGoalEntity::class, BudgetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ContaMeDatabase : RoomDatabase() {
    /** Acceso al DAO de transacciones */
    abstract fun transactionDao(): TransactionDao
    /** Acceso al DAO de metas de ahorro */
    abstract fun savingsGoalDao(): SavingsGoalDao
    /** Acceso al DAO de presupuestos mensuales */
    abstract fun budgetDao(): BudgetDao
}
