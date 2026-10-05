package com.uma.contame.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uma.contame.auth.AuthManager
import com.uma.contame.auth.UserProfile
import com.uma.contame.data.repository.ContaMeRepository
import com.uma.contame.model.CategoryBreakdown
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.MonthlyStat
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import com.uma.contame.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Estado inmutable de la interfaz de usuario para la aplicación `contaME`.
 *
 * Reúne todas las variables reactivas necesarias para renderizar las pantallas sin
 * realizar cálculos costosos dentro de los Composables.
 *
 * @property transactions Lista completa de transacciones del usuario.
 * @property filteredTransactions Lista de transacciones filtradas por tipo, categoría o búsqueda.
 * @property savingsGoals Lista de metas de ahorro del usuario.
 * @property monthlyBudget Configuración del presupuesto mensual del mes activo.
 * @property selectedMonthKey Clave del mes seleccionado en formato "yyyy-MM".
 * @property selectedMonthLabel Etiqueta visible del mes (ej: "Septiembre 2026").
 * @property currentBalance Balance histórico acumulado (Ingresos - Gastos).
 * @property monthlyIncome Total de ingresos registrados en el mes actual.
 * @property monthlyExpense Total de gastos registrados en el mes actual.
 * @property budgetUsagePercent Porcentaje consumido del presupuesto mensual (de 0.0f a 1.0f).
 * @property isBudgetExceeded Verdadero si los gastos del mes superaron el presupuesto.
 * @property isBudgetWarning Verdadero si los gastos alcanzaron el umbral de alerta configurado.
 * @property expenseCategoriesBreakdown Desglose porcentual de gastos por categoría para el gráfico de dona.
 * @property incomeCategoriesBreakdown Desglose porcentual de ingresos por categoría para el gráfico de dona.
 * @property monthlyComparison Histórico comparativo de Ingresos vs Gastos para el gráfico de barras.
 * @property isCloudSyncing Indica si hay un proceso de sincronización con la nube en marcha.
 * @property cloudSyncMessage Mensaje explicativo sobre el estado de la sincronización.
 * @property activeFilter Filtro activo por tipo de movimiento (null = todos, Gasto o Ingreso).
 * @property searchQuery Texto de búsqueda ingresado por el usuario.
 * @property selectedCategoryFilter ID de la categoría seleccionada para filtrar.
 * @property userProfile Perfil del usuario autenticado actual.
 * @property isAuthLoading Indica si se está autenticando con Google/Firebase.
 * @property authError Mensaje de error de autenticación en caso de falla.
 */
data class ContaMeUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val filteredTransactions: List<TransactionItem> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val monthlyBudget: MonthlyBudget = MonthlyBudget(
        monthYearKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
        budgetLimit = 1200.0,
        alertThresholdPercent = 80,
        notificationsEnabled = true
    ),
    val selectedMonthKey: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    val selectedMonthLabel: String = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES")).format(Date()).replaceFirstChar { it.uppercase() },
    val currentBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val budgetUsagePercent: Float = 0.0f,
    val isBudgetExceeded: Boolean = false,
    val isBudgetWarning: Boolean = false,
    val expenseCategoriesBreakdown: List<CategoryBreakdown> = emptyList(),
    val incomeCategoriesBreakdown: List<CategoryBreakdown> = emptyList(),
    val monthlyComparison: List<MonthlyStat> = emptyList(),
    val isCloudSyncing: Boolean = false,
    val cloudSyncMessage: String? = null,
    val activeFilter: TransactionType? = null,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val userProfile: UserProfile? = null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null
)

/**
 * ViewModel principal de la aplicación `contaME`.
 *
 * Actúa como intermediario entre la capa de datos ([ContaMeRepository] y [AuthManager]) y la capa
 * de presentación en Jetpack Compose ([ContaMeUiState]).
 *
 * Mantiene el estado en un [StateFlow] unificado que combina las consultas locales y remotas
 * y reacciona automáticamente ante cambios en tiempo real.
 *
 * @param application Contexto de la aplicación Android.
 */
class ContaMeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContaMeRepository.getInstance(application)
    private val authManager = AuthManager.getInstance(application)
    private val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Estados internos privados para los filtros de búsqueda e interfaz
    private val _activeFilter = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _cloudSyncing = MutableStateFlow(false)
    private val _cloudMessage = MutableStateFlow<String?>("Sincronizado con base de datos local y Firebase")

    /**
     * Estado público e inmutable expuesto a los Composables como [StateFlow].
     *
     * Se combina reactivamente a partir de 11 fuentes de información en tiempo real.
     */
    val uiState: StateFlow<ContaMeUiState> = combine(
        repository.transactions,
        repository.savingsGoals,
        repository.getBudgetForMonth(currentMonthKey),
        _activeFilter,
        _searchQuery,
        _categoryFilter,
        _cloudSyncing,
        _cloudMessage,
        authManager.currentUser,
        authManager.isLoading,
        authManager.authError
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<TransactionItem>
        @Suppress("UNCHECKED_CAST")
        val goals = args[1] as List<SavingsGoal>
        val budget = (args[2] as? MonthlyBudget) ?: MonthlyBudget(currentMonthKey, 1200.0, 80, true)
        val filter = args[3] as? TransactionType
        val query = args[4] as String
        val catFilter = args[5] as? String
        val syncing = args[6] as Boolean
        val message = args[7] as? String
        val user = args[8] as? UserProfile
        val authLoading = args[9] as Boolean
        val authErr = args[10] as? String

        calculateUiState(
            transactions = transactions,
            goals = goals,
            budget = budget,
            filter = filter,
            query = query,
            catFilter = catFilter,
            syncing = syncing,
            message = message,
            user = user,
            authLoading = authLoading,
            authErr = authErr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContaMeUiState()
    )

    init {
        // Inicializar canal de notificaciones en el sistema Android
        NotificationHelper.createNotificationChannel(application)

        // Escuchar cambios de sesión y sincronizar con la cuenta activa
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                repository.switchUser(user?.uid)
            }
        }
    }

    /**
     * Realiza todos los cálculos financieros (balances, promedios, porcentajes de consumo
     * del presupuesto y desgloses de categorías) para construir una copia limpia de [ContaMeUiState].
     */
    private fun calculateUiState(
        transactions: List<TransactionItem>,
        goals: List<SavingsGoal>,
        budget: MonthlyBudget,
        filter: TransactionType?,
        query: String,
        catFilter: String?,
        syncing: Boolean,
        message: String?,
        user: UserProfile?,
        authLoading: Boolean,
        authErr: String?
    ): ContaMeUiState {
        val now = Date()
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES"))
        val currentKey = monthKeyFormat.format(now)
        val monthLabel = monthDisplayFormat.format(now).replaceFirstChar { it.uppercase() }

        // Filtrar transacciones del mes en curso para los cálculos presupuestarios
        val currentMonthTransactions = transactions.filter {
            monthKeyFormat.format(Date(it.timestamp)) == currentKey
        }

        val totalIncome = currentMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val allTimeIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allTimeExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = allTimeIncome - allTimeExpense

        val usagePercent = if (budget.budgetLimit > 0) {
            (totalExpense / budget.budgetLimit).toFloat()
        } else 0f

        val isExceeded = totalExpense > budget.budgetLimit
        val isWarning = !isExceeded && (usagePercent >= (budget.alertThresholdPercent / 100f))

        // Desglose porcentual para gastos por categoría
        val expensesCurrentMonth = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }
        val expenseBreakdown = expensesCurrentMonth.groupBy { it.categoryId }
            .map { (_, items) ->
                val sum = items.sumOf { it.amount }
                val first = items.first()
                val pct = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f
                CategoryBreakdown(
                    categoryName = first.categoryName,
                    categoryIcon = first.categoryIcon,
                    colorHex = first.categoryColor,
                    totalAmount = sum,
                    percentage = pct
                )
            }.sortedByDescending { it.totalAmount }

        // Desglose porcentual para ingresos por categoría
        val incomesCurrentMonth = currentMonthTransactions.filter { it.type == TransactionType.INCOME }
        val incomeBreakdown = incomesCurrentMonth.groupBy { it.categoryId }
            .map { (_, items) ->
                val sum = items.sumOf { it.amount }
                val first = items.first()
                val pct = if (totalIncome > 0) (sum / totalIncome).toFloat() else 0f
                CategoryBreakdown(
                    categoryName = first.categoryName,
                    categoryIcon = first.categoryIcon,
                    colorHex = first.categoryColor,
                    totalAmount = sum,
                    percentage = pct
                )
            }.sortedByDescending { it.totalAmount }

        // Comparativa histórica de los últimos 4 meses
        val monthlyComparison = buildMonthlyComparison(transactions)

        // Aplicar filtros activos (búsqueda, tipo y categoría) sobre el historial de transacciones
        val filtered = transactions.filter { item ->
            val matchesType = (filter == null || item.type == filter)
            val matchesCategory = (catFilter == null || item.categoryId == catFilter)
            val matchesQuery = query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)
            matchesType && matchesCategory && matchesQuery
        }

        return ContaMeUiState(
            transactions = transactions,
            filteredTransactions = filtered,
            savingsGoals = goals,
            monthlyBudget = budget,
            selectedMonthKey = currentKey,
            selectedMonthLabel = monthLabel,
            currentBalance = balance,
            monthlyIncome = totalIncome,
            monthlyExpense = totalExpense,
            budgetUsagePercent = usagePercent,
            isBudgetExceeded = isExceeded,
            isBudgetWarning = isWarning,
            expenseCategoriesBreakdown = expenseBreakdown,
            incomeCategoriesBreakdown = incomeBreakdown,
            monthlyComparison = monthlyComparison,
            isCloudSyncing = syncing,
            cloudSyncMessage = message,
            activeFilter = filter,
            searchQuery = query,
            selectedCategoryFilter = catFilter,
            userProfile = user,
            isAuthLoading = authLoading,
            authError = authErr
        )
    }

    /**
     * Inicia el proceso de autenticación con Google.
     */
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            authManager.signInWithGoogle(activity)
        }
    }

    /**
     * Autentica con correo electrónico y contraseña en Firebase Auth.
     */
    fun authenticateWithFirebaseEmail(email: String, pass: String, name: String) {
        viewModelScope.launch {
            authManager.authenticateWithFirebaseEmail(email, pass, name)
        }
    }

    /**
     * Cierra la sesión activa del usuario y cambia al modo local.
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.switchUser("local")
        }
    }

    /**
     * Construye la lista histórica de comparativa mensual de Ingresos vs Gastos para los últimos 4 meses.
     */
    private fun buildMonthlyComparison(transactions: List<TransactionItem>): List<MonthlyStat> {
        val stats = mutableListOf<MonthlyStat>()
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.forLanguageTag("es-ES"))

        for (i in 3 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val key = monthKeyFormat.format(cal.time)
            val label = shortMonthFormat.format(cal.time).replaceFirstChar { it.uppercase() }

            val monthItems = transactions.filter {
                monthKeyFormat.format(Date(it.timestamp)) == key
            }
            val inc = monthItems.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = monthItems.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            stats.add(MonthlyStat(monthLabel = label, income = inc, expense = exp))
        }
        return stats
    }

    /**
     * Establece el filtro de tipo de movimiento (null = todos, EXPENSE o INCOME).
     */
    fun setFilter(type: TransactionType?) {
        _activeFilter.value = type
    }

    /**
     * Establece la consulta de texto para buscar en las transacciones.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Filtra las transacciones por una categoría específica.
     */
    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
    }

    /**
     * Registra un nuevo movimiento o actualiza uno existente, verificando el presupuesto para alertas.
     */
    fun addOrUpdateTransaction(item: TransactionItem, context: Context) {
        viewModelScope.launch {
            repository.saveTransaction(item)

            if (item.type == TransactionType.EXPENSE) {
                checkAndDispatchBudgetAlert(context, item.categoryName)
            }
        }
    }

    /**
     * Elimina una transacción por su ID.
     */
    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    /**
     * Registra o modifica una meta de ahorro.
     */
    fun addOrUpdateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.saveGoal(goal)
        }
    }

    /**
     * Elimina una meta de ahorro por su ID.
     */
    fun deleteGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    /**
     * Añade fondos acumulados a una meta de ahorro y opcionalmente lo registra como gasto en el presupuesto.
     */
    fun addFundsToGoal(
        goal: SavingsGoal,
        amount: Double,
        registerAsExpense: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.saveGoal(updatedGoal)

            if (registerAsExpense) {
                val savingsExpense = TransactionItem(
                    title = "Ahorro: ${goal.title}",
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = "exp_savings",
                    categoryName = "Ahorro e Inversión",
                    categoryIcon = "savings",
                    categoryColor = "#0284C7",
                    timestamp = System.currentTimeMillis(),
                    notes = "Aporte a meta: ${goal.title}"
                )
                repository.saveTransaction(savingsExpense)
                checkAndDispatchBudgetAlert(context, "Ahorro e Inversión")
            }
        }
    }

    /**
     * Actualiza el límite de presupuesto del mes, el porcentaje umbral de alerta y notificaciones.
     */
    fun updateMonthlyBudget(
        limit: Double,
        thresholdPercent: Int,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            val updated = uiState.value.monthlyBudget.copy(
                budgetLimit = limit,
                alertThresholdPercent = thresholdPercent,
                notificationsEnabled = notificationsEnabled
            )
            repository.saveBudget(updated)
        }
    }

    /**
     * Fuerza la sincronización en segundo plano con la base de datos remota Firebase Firestore.
     */
    fun syncWithCloud() {
        viewModelScope.launch {
            _cloudSyncing.value = true
            _cloudMessage.value = "Sincronizando con Firebase..."
            val result = repository.syncFromCloud()
            _cloudSyncing.value = false
            if (result.isSuccess) {
                _cloudMessage.value = "Sincronización exitosa con Firebase Firestore"
            } else {
                _cloudMessage.value = "Guardado local activo (Firebase pendiente: ${result.exceptionOrNull()?.message ?: "sin conexión"})"
            }
        }
    }

    /**
     * Verifica si los gastos actuales han superado el presupuesto mensual o el umbral de alerta,
     * y emite la notificación push correspondiente mediante [NotificationHelper].
     */
    private fun checkAndDispatchBudgetAlert(context: Context, categoryName: String) {
        val state = uiState.value
        val budget = state.monthlyBudget
        if (!budget.notificationsEnabled) return

        val totalExpenses = state.monthlyExpense
        val limit = budget.budgetLimit

        if (totalExpenses > limit) {
            NotificationHelper.sendBudgetExceededNotification(
                context = context,
                totalExpenses = totalExpenses,
                budgetLimit = limit,
                topCategory = categoryName
            )
        } else {
            val pct = if (limit > 0) ((totalExpenses / limit) * 100).toInt() else 0
            if (pct >= budget.alertThresholdPercent) {
                NotificationHelper.sendBudgetWarningNotification(
                    context = context,
                    percentage = pct,
                    totalExpenses = totalExpenses,
                    budgetLimit = limit
                )
            }
        }
    }
}
