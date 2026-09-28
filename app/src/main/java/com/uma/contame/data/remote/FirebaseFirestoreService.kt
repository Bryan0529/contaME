package com.uma.contame.data.remote

import android.util.Log
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class FirebaseFirestoreService {

    private val tag = "ContaMeFirebase"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseFirestore instance not ready or not configured: ${e.message}")
            null
        }
    }

    fun isAvailable(): Boolean = firestore != null

    private fun userTransactions(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("transactions")

    private fun userGoals(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("savings_goals")

    private fun userBudgets(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("monthly_budgets")

    suspend fun saveUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        val doc = firestore?.collection("users")?.document(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val data = hashMapOf(
                "propietarioUid" to userId,
                "userId" to userId,
                "email" to (email ?: ""),
                "displayName" to (displayName ?: ""),
                "photoUrl" to (photoUrl ?: ""),
                "lastLogin" to System.currentTimeMillis()
            )
            doc.set(data, SetOptions.merge()).await()
            Log.d(tag, "Perfil guardado en Firebase para propietarioUid [$userId]")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando perfil [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveTransaction(userId: String, item: TransactionItem): Result<Unit> {
        val col = userTransactions(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "id" to item.id,
                "userId" to userId,
                "propietarioUid" to userId,
                "title" to item.title,
                "amount" to item.amount,
                "type" to item.type.name,
                "categoryId" to item.categoryId,
                "categoryName" to item.categoryName,
                "categoryIcon" to item.categoryIcon,
                "categoryColor" to item.categoryColor,
                "timestamp" to item.timestamp,
                "notes" to item.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            col.document(item.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(tag, "Transacción guardada en Firebase para propietarioUid [$userId]: ${item.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando transacción en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, id: String): Result<Unit> {
        val col = userTransactions(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            col.document(id)
                .delete()
                .await()
            Log.d(tag, "Transacción eliminada en Firebase para usuario [$userId]: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando transacción en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveGoal(userId: String, goal: SavingsGoal): Result<Unit> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "id" to goal.id,
                "userId" to userId,
                "propietarioUid" to userId,
                "title" to goal.title,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "deadlineTimestamp" to (goal.deadlineTimestamp ?: 0L),
                "colorHex" to goal.colorHex,
                "iconName" to goal.iconName,
                "updatedAt" to System.currentTimeMillis()
            )
            col.document(goal.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(tag, "Meta de ahorro guardada en Firebase para propietarioUid [$userId]: ${goal.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando meta en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(userId: String, id: String): Result<Unit> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            col.document(id)
                .delete()
                .await()
            Log.d(tag, "Meta eliminada en Firebase para propietarioUid [$userId]: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando meta en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveBudget(userId: String, budget: MonthlyBudget): Result<Unit> {
        val col = userBudgets(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "monthYearKey" to budget.monthYearKey,
                "userId" to userId,
                "propietarioUid" to userId,
                "budgetLimit" to budget.budgetLimit,
                "alertThresholdPercent" to budget.alertThresholdPercent,
                "notificationsEnabled" to budget.notificationsEnabled,
                "updatedAt" to System.currentTimeMillis()
            )
            col.document(budget.monthYearKey)
                .set(data, SetOptions.merge())
                .await()
            Log.d(tag, "Presupuesto guardado en Firebase para propietarioUid [$userId]: ${budget.monthYearKey}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando presupuesto en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllTransactions(userId: String): Result<List<TransactionItem>> {
        val col = userTransactions(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val snapshot = col.get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val typeStr = doc.getString("type") ?: "EXPENSE"
                val categoryId = doc.getString("categoryId") ?: "exp_other"
                val categoryName = doc.getString("categoryName") ?: "Otros"
                val categoryIcon = doc.getString("categoryIcon") ?: "more_horiz"
                val categoryColor = doc.getString("categoryColor") ?: "#64748B"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val notes = doc.getString("notes") ?: ""

                TransactionItem(
                    id = id,
                    title = title,
                    amount = amount,
                    type = if (typeStr == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    categoryColor = categoryColor,
                    timestamp = timestamp,
                    notes = notes
                )
            }
            Log.d(tag, "Descargadas ${items.size} transacciones desde Firebase para usuario [$userId]")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "Error descargando transacciones de Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllGoals(userId: String): Result<List<SavingsGoal>> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val snapshot = col.get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val targetAmount = doc.getDouble("targetAmount") ?: 0.0
                val currentAmount = doc.getDouble("currentAmount") ?: 0.0
                val deadline = doc.getLong("deadlineTimestamp").let { if (it == null || it == 0L) null else it }
                val colorHex = doc.getString("colorHex") ?: "#3B82F6"
                val iconName = doc.getString("iconName") ?: "flag"

                SavingsGoal(
                    id = id,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineTimestamp = deadline,
                    colorHex = colorHex,
                    iconName = iconName
                )
            }
            Log.d(tag, "Descargadas ${items.size} metas desde Firebase para usuario [$userId]")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "Error descargando metas de Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchBudget(userId: String, monthYearKey: String): Result<MonthlyBudget?> {
        val col = userBudgets(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val doc = col.document(monthYearKey).get().await()
            if (doc.exists()) {
                val budget = MonthlyBudget(
                    monthYearKey = monthYearKey,
                    budgetLimit = doc.getDouble("budgetLimit") ?: 1200.0,
                    alertThresholdPercent = (doc.getLong("alertThresholdPercent") ?: 80L).toInt(),
                    notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true
                )
                Result.success(budget)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
