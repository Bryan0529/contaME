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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Servicio remoto para sincronización y almacenamiento persistente en **Firebase Firestore**.
 *
 * Administra la comunicación en la nube para perfiles de usuario, transacciones, metas de ahorro
 * y presupuestos mensuales. Además, realiza migraciones automáticas entre esquemas de colecciones.
 */
class FirebaseFirestoreService {

    private val tag = "ContaMeFirebase"

    /**
     * Retorna la fecha y hora actual en un formato de cadena legible en español.
     */
    private fun fechaActualFormateada(): String {
        return try {
            val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm:ss", Locale.forLanguageTag("es-ES"))
            sdf.format(Date())
        } catch (_: Exception) {
            Date().toString()
        }
    }

    // Instancia de FirebaseFirestore obtenida de forma perezosa y segura
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Instancia de FirebaseFirestore no disponible o no configurada: ${e.message}")
            null
        }
    }

    /**
     * Verifica si el servicio de Firestore está disponible y listo para operar.
     */
    fun isAvailable(): Boolean = firestore != null

    // Referencias a las subcolecciones por usuario en la estructura de Firestore
    private fun userTransactions(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("transacciones")

    private fun userGoals(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("metas_ahorro")

    private fun userBudgets(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("presupuestos_mensuales")

    /**
     * Garantiza que la estructura del documento del usuario exista en Firestore y migra datos si es necesario.
     */
    suspend fun ensureUserStructure(
        userId: String,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        val res = saveUserProfile(userId, email, displayName, photoUrl)
        try {
            syncAndMigrateUserData(userId)
        } catch (_: Exception) {}
        return res
    }

    /**
     * Guarda o actualiza los datos del perfil del usuario en la colección raíz `users/{userId}`.
     */
    suspend fun saveUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        val doc = firestore?.collection("users")?.document(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val ahoraFormateado = fechaActualFormateada()
            val data = hashMapOf(
                "usuarioId" to userId,
                "propietarioUid" to userId,
                "email" to (email ?: ""),
                "nombreMostrado" to (displayName ?: ""),
                "fotoUrl" to (photoUrl ?: ""),
                "ultimoInicioSesion" to ahoraFormateado,
                "actualizadoEn" to ahoraFormateado
            )
            doc.set(data, SetOptions.merge()).await()
            Log.d(tag, "Perfil guardado en Firebase para usuarioId [$userId]")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando perfil [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza y guarda una transacción en la subcolección `users/{userId}/transacciones/{id}`.
     */
    suspend fun saveTransaction(userId: String, item: TransactionItem): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "id" to item.id,
                "usuarioId" to userId,
                "propietarioUid" to userId,
                "titulo" to item.title,
                "monto" to item.amount,
                "tipo" to item.type.name,
                "categoriaId" to item.categoryId,
                "nombreCategoria" to item.categoryName,
                "iconoCategoria" to item.categoryIcon,
                "colorCategoria" to item.categoryColor,
                "fechaHora" to item.timestamp,
                "notas" to item.notes,
                "actualizadoEn" to fechaActualFormateada()
            )
            // Guardar en la subcolección estandarizada en español
            fs.collection("users").document(userId).collection("transacciones")
                .document(item.id)
                .set(data)
                .await()

            Log.d(tag, "Transacción guardada en users/$userId/transacciones: ${item.title} ($${item.amount})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando transacción en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina una transacción en Firestore (tanto en la colección actual como en las anteriores).
     */
    suspend fun deleteTransaction(userId: String, id: String): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            fs.collection("users").document(userId).collection("transacciones").document(id).delete().await()
            try {
                fs.collection("users").document(userId).collection("transactions").document(id).delete().await()
            } catch (_: Exception) {}

            Log.d(tag, "Transacción eliminada en users/$userId/transacciones: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando transacción en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza una meta de ahorro en `users/{userId}/metas_ahorro/{id}`.
     */
    suspend fun saveGoal(userId: String, goal: SavingsGoal): Result<Unit> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "id" to goal.id,
                "usuarioId" to userId,
                "propietarioUid" to userId,
                "titulo" to goal.title,
                "montoObjetivo" to goal.targetAmount,
                "montoActual" to goal.currentAmount,
                "fechaLimite" to (goal.deadlineTimestamp ?: 0L),
                "colorHex" to goal.colorHex,
                "nombreIcono" to goal.iconName,
                "actualizadoEn" to fechaActualFormateada()
            )
            col.document(goal.id)
                .set(data)
                .await()

            Log.d(tag, "Meta de ahorro guardada en users/$userId/metas_ahorro: ${goal.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando meta en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina una meta de ahorro en Firestore.
     */
    suspend fun deleteGoal(userId: String, id: String): Result<Unit> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            col.document(id).delete().await()
            try {
                firestore?.collection("users")?.document(userId)?.collection("savings_goals")?.document(id)?.delete()?.await()
            } catch (_: Exception) {}

            Log.d(tag, "Meta eliminada en users/$userId/metas_ahorro: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando meta en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza la configuración del presupuesto mensual en `users/{userId}/presupuestos_mensuales/{monthYearKey}`.
     */
    suspend fun saveBudget(userId: String, budget: MonthlyBudget): Result<Unit> {
        val col = userBudgets(userId) ?: return Result.failure(Exception("Firebase no está configurado"))
        return try {
            val data = hashMapOf(
                "claveMesAno" to budget.monthYearKey,
                "usuarioId" to userId,
                "propietarioUid" to userId,
                "limitePresupuesto" to budget.budgetLimit,
                "porcentajeAlerta" to budget.alertThresholdPercent,
                "notificacionesHabilitadas" to budget.notificationsEnabled,
                "actualizadoEn" to fechaActualFormateada()
            )
            col.document(budget.monthYearKey)
                .set(data)
                .await()

            Log.d(tag, "Presupuesto guardado en users/$userId/presupuestos_mensuales: ${budget.monthYearKey}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando presupuesto en Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Ejecuta un proceso de migración para mover datos desde nombres de colecciones antiguos en inglés
     * (`transactions`, `savings_goals`, `monthly_budgets`) hacia el esquema unificado en español.
     */
    suspend fun syncAndMigrateUserData(userId: String) {
        val fs = firestore ?: return
        try {
            val userDocRef = fs.collection("users").document(userId)

            // 1. Asegurar documento del usuario
            userDocRef.set(
                hashMapOf(
                    "usuarioId" to userId,
                    "propietarioUid" to userId,
                    "actualizadoEn" to fechaActualFormateada()
                ),
                SetOptions.merge()
            ).await()

            // 2. Migrar presupuestos
            val oldBudgets = userDocRef.collection("monthly_budgets").get().await()
            for (doc in oldBudgets.documents) {
                val monthKey = doc.getString("claveMesAno") ?: doc.getString("monthYearKey") ?: doc.id
                val limit = doc.getDouble("limitePresupuesto") ?: doc.getDouble("budgetLimit") ?: 1200.0
                val threshold = (doc.getLong("porcentajeAlerta") ?: doc.getLong("alertThresholdPercent") ?: 80L).toInt()
                val notif = doc.getBoolean("notificacionesHabilitadas") ?: doc.getBoolean("notificationsEnabled") ?: true

                val budget = MonthlyBudget(
                    monthYearKey = monthKey,
                    budgetLimit = limit,
                    alertThresholdPercent = threshold,
                    notificationsEnabled = notif
                )
                saveBudget(userId, budget)
                Log.d(tag, "Presupuesto '$monthKey' migrado a users/$userId/presupuestos_mensuales")
            }

            // 3. Migrar transacciones
            val oldTx = userDocRef.collection("transactions").get().await()
            for (doc in oldTx.documents) {
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("titulo") ?: doc.getString("title") ?: continue
                val amount = doc.getDouble("monto") ?: doc.getDouble("amount") ?: 0.0
                val typeStr = doc.getString("tipo") ?: doc.getString("type") ?: "EXPENSE"
                val catId = doc.getString("categoriaId") ?: doc.getString("categoryId") ?: "exp_other"
                val catName = doc.getString("nombreCategoria") ?: doc.getString("categoryName") ?: "Otros"
                val catIcon = doc.getString("iconoCategoria") ?: doc.getString("categoryIcon") ?: "more_horiz"
                val catColor = doc.getString("colorCategoria") ?: doc.getString("categoryColor") ?: "#64748B"
                val timestamp = doc.getLong("fechaHora") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                val notes = doc.getString("notas") ?: doc.getString("notes") ?: ""

                val tx = TransactionItem(
                    id = id,
                    title = title,
                    amount = amount,
                    type = if (typeStr == "INCOME" || typeStr == "INGRESO") TransactionType.INCOME else TransactionType.EXPENSE,
                    categoryId = catId,
                    categoryName = catName,
                    categoryIcon = catIcon,
                    categoryColor = catColor,
                    timestamp = timestamp,
                    notes = notes
                )
                saveTransaction(userId, tx)
            }

            // 4. Migrar metas de ahorro
            val oldGoals = userDocRef.collection("savings_goals").get().await()
            for (doc in oldGoals.documents) {
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("titulo") ?: doc.getString("title") ?: continue
                val targetAmount = doc.getDouble("montoObjetivo") ?: doc.getDouble("montoMeta") ?: doc.getDouble("targetAmount") ?: 0.0
                val currentAmount = doc.getDouble("montoActual") ?: doc.getDouble("currentAmount") ?: 0.0
                val deadline = (doc.getLong("fechaLimite") ?: doc.getLong("deadlineTimestamp")).let { if (it == null || it == 0L) null else it }
                val colorHex = doc.getString("colorHex") ?: "#3B82F6"
                val iconName = doc.getString("nombreIcono") ?: doc.getString("iconName") ?: "flag"

                val goal = SavingsGoal(
                    id = id,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineTimestamp = deadline,
                    colorHex = colorHex,
                    iconName = iconName
                )
                saveGoal(userId, goal)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error en syncAndMigrateUserData [$userId]: ${e.message}", e)
        }
    }

    /**
     * Descarga todas las transacciones registradas del usuario desde Firestore.
     */
    suspend fun fetchAllTransactions(userId: String): Result<List<TransactionItem>> {
        val col = userTransactions(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val snapshot = col.get().await()
            val docs = if (snapshot.isEmpty) {
                val oldSnapshot = firestore?.collection("users")?.document(userId)?.collection("transactions")?.get()?.await()
                if (oldSnapshot != null && !oldSnapshot.isEmpty) {
                    oldSnapshot.documents
                } else {
                    snapshot.documents
                }
            } else {
                snapshot.documents
            }

            val items = docs.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("titulo") ?: doc.getString("title") ?: return@mapNotNull null
                val amount = doc.getDouble("monto") ?: doc.getDouble("amount") ?: 0.0
                val typeStr = doc.getString("tipo") ?: doc.getString("type") ?: "EXPENSE"
                val categoryId = doc.getString("categoriaId") ?: doc.getString("categoryId") ?: "exp_other"
                val categoryName = doc.getString("nombreCategoria") ?: doc.getString("categoryName") ?: "Otros"
                val categoryIcon = doc.getString("iconoCategoria") ?: doc.getString("categoryIcon") ?: "more_horiz"
                val categoryColor = doc.getString("colorCategoria") ?: doc.getString("categoryColor") ?: "#64748B"
                val timestamp = doc.getLong("fechaHora") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                val notes = doc.getString("notas") ?: doc.getString("notes") ?: ""

                TransactionItem(
                    id = id,
                    title = title,
                    amount = amount,
                    type = if (typeStr == "INCOME" || typeStr == "INGRESO") TransactionType.INCOME else TransactionType.EXPENSE,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    categoryColor = categoryColor,
                    timestamp = timestamp,
                    notes = notes
                )
            }
            Log.d(tag, "Descargadas ${items.size} transacciones desde Firebase para usuario [$userId]")
            if (snapshot.isEmpty && items.isNotEmpty()) {
                items.forEach { item ->
                    try { saveTransaction(userId, item) } catch (_: Exception) {}
                }
                Log.d(tag, "Migradas automáticamente ${items.size} transacciones a 'transacciones'")
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "Error descargando transacciones de Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Descarga todas las metas de ahorro del usuario desde Firestore.
     */
    suspend fun fetchAllGoals(userId: String): Result<List<SavingsGoal>> {
        val col = userGoals(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            val snapshot = col.get().await()
            val docs = if (snapshot.isEmpty) {
                val oldSnapshot = firestore?.collection("users")?.document(userId)?.collection("savings_goals")?.get()?.await()
                if (oldSnapshot != null && !oldSnapshot.isEmpty) {
                    oldSnapshot.documents
                } else {
                    snapshot.documents
                }
            } else {
                snapshot.documents
            }

            val items = docs.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("titulo") ?: doc.getString("title") ?: return@mapNotNull null
                val targetAmount = doc.getDouble("montoObjetivo") ?: doc.getDouble("montoMeta") ?: doc.getDouble("targetAmount") ?: 0.0
                val currentAmount = doc.getDouble("montoActual") ?: doc.getDouble("currentAmount") ?: 0.0
                val deadline = (doc.getLong("fechaLimite") ?: doc.getLong("deadlineTimestamp")).let { if (it == null || it == 0L) null else it }
                val colorHex = doc.getString("colorHex") ?: "#3B82F6"
                val iconName = doc.getString("nombreIcono") ?: doc.getString("iconName") ?: "flag"

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
            if (snapshot.isEmpty && items.isNotEmpty()) {
                items.forEach { goal ->
                    try { saveGoal(userId, goal) } catch (_: Exception) {}
                }
                Log.d(tag, "Migradas automáticamente ${items.size} metas a 'metas_ahorro'")
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "Error descargando metas de Firebase [$userId]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el presupuesto correspondiente a un mes concreto para el usuario desde Firestore.
     */
    suspend fun fetchBudget(userId: String, monthYearKey: String): Result<MonthlyBudget?> {
        val col = userBudgets(userId) ?: return Result.failure(Exception("Firebase no disponible"))
        return try {
            var doc = col.document(monthYearKey).get().await()
            if (!doc.exists()) {
                val oldDoc = firestore?.collection("users")?.document(userId)?.collection("monthly_budgets")?.document(monthYearKey)?.get()?.await()
                if (oldDoc != null && oldDoc.exists()) {
                    doc = oldDoc
                }
            }
            if (doc.exists()) {
                val budget = MonthlyBudget(
                    monthYearKey = monthYearKey,
                    budgetLimit = doc.getDouble("limitePresupuesto") ?: doc.getDouble("budgetLimit") ?: 1200.0,
                    alertThresholdPercent = (doc.getLong("porcentajeAlerta") ?: doc.getLong("alertThresholdPercent") ?: 80L).toInt(),
                    notificationsEnabled = doc.getBoolean("notificacionesHabilitadas") ?: doc.getBoolean("notificationsEnabled") ?: true
                )
                if (col.document(monthYearKey).get().await().exists().not()) {
                    try {
                        saveBudget(userId, budget)
                        Log.d(tag, "Presupuesto $monthYearKey migrado automáticamente a 'presupuestos_mensuales'")
                    } catch (_: Exception) {}
                }
                Result.success(budget)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
