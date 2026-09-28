package com.uma.contame.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.uma.contame.data.local.BudgetEntity
import com.uma.contame.data.local.ContaMeDatabase
import com.uma.contame.data.local.SavingsGoalEntity
import com.uma.contame.data.local.TransactionEntity
import com.uma.contame.data.remote.FirebaseFirestoreService
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ContaMeRepository private constructor(
    private val database: ContaMeDatabase,
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {
    private val tag = "ContaMeRepo"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _currentUserId = MutableStateFlow("local")
    val currentUserId = _currentUserId.asStateFlow()

    val transactions: Flow<List<TransactionItem>> = _currentUserId.flatMapLatest { uid ->
        database.transactionDao().getAllTransactions(uid)
    }.map { list -> list.map { it.toDomain() } }

    val savingsGoals: Flow<List<SavingsGoal>> = _currentUserId.flatMapLatest { uid ->
        database.savingsGoalDao().getAllGoals(uid)
    }.map { list -> list.map { it.toDomain() } }

    fun getBudgetForMonth(monthYearKey: String): Flow<MonthlyBudget?> {
        return _currentUserId.flatMapLatest { uid ->
            database.budgetDao().getBudgetForMonth(monthYearKey, uid)
        }.map { it?.toDomain() }
    }

    fun switchUser(userId: String?) {
        val targetUid = userId?.trim()?.ifEmpty { "local" } ?: "local"
        if (_currentUserId.value == targetUid) return

        Log.d(tag, "Cambiando de usuario: [${_currentUserId.value}] -> [$targetUid]")
        _currentUserId.value = targetUid

        // Trigger cloud sync and upload local transactions for the newly selected user
        if (targetUid != "local") {
            coroutineScope.launch {
                pushAllLocalToCloud(targetUid)
                syncFromCloud(targetUid)
            }
        }
    }

    suspend fun pushAllLocalToCloud(userId: String) = withContext(Dispatchers.IO) {
        if (userId == "local") return@withContext
        try {
            val allTx = database.transactionDao().getAllTransactionsAnyUser()
            for (tx in allTx) {
                if (tx.userId == "local") {
                    database.transactionDao().insertOrUpdate(tx.copy(userId = userId))
                }
                firestoreService.saveTransaction(userId, tx.toDomain())
            }
            Log.d(tag, "pushAllLocalToCloud: ${allTx.size} transacciones sincronizadas a Firebase para $userId")
        } catch (e: Exception) {
            Log.w(tag, "Error en pushAllLocalToCloud: ${e.message}")
        }
    }

    suspend fun saveTransaction(item: TransactionItem): Result<Unit> = withContext(Dispatchers.IO) {
        val authUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
        val uid = when {
            _currentUserId.value != "local" -> _currentUserId.value
            !authUid.isNullOrEmpty() -> {
                _currentUserId.value = authUid
                authUid
            }
            else -> "local"
        }

        // 1. Guardar localmente en Room
        database.transactionDao().insertOrUpdate(TransactionEntity.fromDomain(item, uid))

        // 2. Sincronizar inmediatamente a Firebase Firestore (tanto en raíz como en subcolección de usuario)
        val targetFirebaseUid = if (uid != "local") uid else authUid
        if (!targetFirebaseUid.isNullOrEmpty() && targetFirebaseUid != "local") {
            try {
                firestoreService.saveTransaction(targetFirebaseUid, item)
            } catch (e: Exception) {
                Log.w(tag, "Local guardado, pero sync Firebase demorado [$targetFirebaseUid]: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun deleteTransaction(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value
        // 1. Delete locally
        database.transactionDao().deleteById(id, uid)

        // 2. Delete in Firebase
        if (uid != "local") {
            try {
                firestoreService.deleteTransaction(uid, id)
            } catch (e: Exception) {
                Log.w(tag, "Local eliminado, pero delete Firebase demorado [$uid]: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun saveGoal(goal: SavingsGoal): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value
        database.savingsGoalDao().insertOrUpdate(SavingsGoalEntity.fromDomain(goal, uid))
        if (uid != "local") {
            try {
                firestoreService.saveGoal(uid, goal)
            } catch (e: Exception) {
                Log.w(tag, "Meta guardada localmente, sync Firebase demorado [$uid]: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun deleteGoal(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value
        database.savingsGoalDao().deleteById(id, uid)
        if (uid != "local") {
            try {
                firestoreService.deleteGoal(uid, id)
            } catch (e: Exception) {
                Log.w(tag, "Meta eliminada localmente, delete Firebase demorado [$uid]: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun addFundsToGoal(goalId: String, amount: Double): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value
        database.savingsGoalDao().addFunds(goalId, uid, amount)
        Result.success(Unit)
    }

    suspend fun saveBudget(budget: MonthlyBudget): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value
        database.budgetDao().insertOrUpdate(BudgetEntity.fromDomain(budget, uid))
        if (uid != "local") {
            try {
                firestoreService.saveBudget(uid, budget)
            } catch (e: Exception) {
                Log.w(tag, "Presupuesto guardado localmente, sync Firebase demorado [$uid]: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun syncFromCloud(userId: String = _currentUserId.value): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (userId == "local") {
                return@withContext Result.success("Modo invitado/local activo")
            }
            if (!firestoreService.isAvailable()) {
                return@withContext Result.failure(Exception("Firebase no está conectado"))
            }

            Log.d(tag, "Iniciando sincronización desde Firebase para usuario [$userId]...")
            val cloudTxRes = firestoreService.fetchAllTransactions(userId)
            if (cloudTxRes.isSuccess) {
                val cloudItems = cloudTxRes.getOrNull() ?: emptyList()
                if (cloudItems.isNotEmpty()) {
                    database.transactionDao().insertAll(cloudItems.map { TransactionEntity.fromDomain(it, userId) })
                }
            }

            val cloudGoalRes = firestoreService.fetchAllGoals(userId)
            if (cloudGoalRes.isSuccess) {
                val cloudGoals = cloudGoalRes.getOrNull() ?: emptyList()
                if (cloudGoals.isNotEmpty()) {
                    database.savingsGoalDao().insertAll(cloudGoals.map { SavingsGoalEntity.fromDomain(it, userId) })
                }
            }

            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val cloudBudgetRes = firestoreService.fetchBudget(userId, currentMonthKey)
            if (cloudBudgetRes.isSuccess) {
                val cloudBudget = cloudBudgetRes.getOrNull()
                if (cloudBudget != null) {
                    database.budgetDao().insertOrUpdate(BudgetEntity.fromDomain(cloudBudget, userId))
                } else {
                    // Initialize default budget for this user if not yet in cloud
                    database.budgetDao().insertOrUpdate(
                        BudgetEntity(
                            id = "$userId-$currentMonthKey",
                            userId = userId,
                            monthYearKey = currentMonthKey,
                            budgetLimit = 1200.0,
                            alertThresholdPercent = 80,
                            notificationsEnabled = true
                        )
                    )
                }
            }

            Result.success("Sincronización con Firebase exitosa para tu cuenta")
        } catch (e: Exception) {
            Log.e(tag, "Error en syncFromCloud [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(now))
        val uid = "local"

        database.budgetDao().insertOrUpdate(
            BudgetEntity(
                id = "$uid-$currentMonthKey",
                userId = uid,
                monthYearKey = currentMonthKey,
                budgetLimit = 1200.0,
                alertThresholdPercent = 80,
                notificationsEnabled = true
            )
        )

        // Seed initial transactions for local/guest preview
        val sampleTransactions = listOf(
            TransactionItem(
                id = UUID.randomUUID().toString(),
                title = "Salario Mensual",
                amount = 1850.0,
                type = TransactionType.INCOME,
                categoryId = "inc_salary",
                categoryName = "Salario",
                categoryIcon = "payments",
                categoryColor = "#10B981",
                timestamp = now - (1000L * 60 * 60 * 24 * 3),
                notes = "Depósito nómina quincenal/mensual"
            ),
            TransactionItem(
                id = UUID.randomUUID().toString(),
                title = "Supermercado Semanal",
                amount = 185.50,
                type = TransactionType.EXPENSE,
                categoryId = "exp_food",
                categoryName = "Alimentación",
                categoryIcon = "restaurant",
                categoryColor = "#EF4444",
                timestamp = now - (1000L * 60 * 60 * 24 * 2),
                notes = "Frutas, verduras, lácteos y víveres"
            ),
            TransactionItem(
                id = UUID.randomUUID().toString(),
                title = "Gasolina y Transporte",
                amount = 65.0,
                type = TransactionType.EXPENSE,
                categoryId = "exp_transport",
                categoryName = "Transporte",
                categoryIcon = "directions_car",
                categoryColor = "#F97316",
                timestamp = now - (1000L * 60 * 60 * 24 * 1),
                notes = "Llenado de tanque vehículo"
            )
        )

        database.transactionDao().insertAll(sampleTransactions.map { TransactionEntity.fromDomain(it, uid) })

        val sampleGoals = listOf(
            SavingsGoal(
                id = UUID.randomUUID().toString(),
                title = "Fondo de Emergencia",
                targetAmount = 2000.0,
                currentAmount = 1250.0,
                deadlineTimestamp = now + (1000L * 60 * 60 * 24 * 120),
                colorHex = "#3B82F6",
                iconName = "security"
            ),
            SavingsGoal(
                id = UUID.randomUUID().toString(),
                title = "Vacaciones",
                targetAmount = 800.0,
                currentAmount = 560.0,
                deadlineTimestamp = now + (1000L * 60 * 60 * 24 * 60),
                colorHex = "#10B981",
                iconName = "flight"
            )
        )

        database.savingsGoalDao().insertAll(sampleGoals.map { SavingsGoalEntity.fromDomain(it, uid) })
    }

    companion object {
        @Volatile
        private var INSTANCE: ContaMeRepository? = null

        fun getInstance(context: Context): ContaMeRepository {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    ContaMeDatabase::class.java,
                    "contame_database"
                ).fallbackToDestructiveMigration().build()

                ContaMeRepository(db).also { repo ->
                    INSTANCE = repo
                    repo.coroutineScope.launch {
                        repo.seedInitialDataIfEmpty()
                    }
                }
            }
        }
    }
}