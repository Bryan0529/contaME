package com.uma.contame.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsAnyUser(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun getTransactionById(id: String, userId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearUserTransactions(userId: String)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    fun getAllGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :addedAmount WHERE id = :id AND userId = :userId")
    suspend fun addFunds(id: String, userId: String, addedAmount: Double)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthYearKey = :key AND userId = :userId LIMIT 1")
    fun getBudgetForMonth(key: String, userId: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)
}

@Database(
    entities = [TransactionEntity::class, SavingsGoalEntity::class, BudgetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ContaMeDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun budgetDao(): BudgetDao
}