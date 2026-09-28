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
import com.uma.contame.model.DefaultCategories
import com.uma.contame.model.MonthlyBudget
import com.uma.contame.model.MonthlyStat
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionCategory
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import com.uma.contame.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val selectedMonthLabel: String = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date()).replaceFirstChar { it.uppercase() },
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
    val activeFilter: TransactionType? = null, // null = all, or EXPENSE / INCOME
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val userProfile: UserProfile? = null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null
)

class ContaMeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContaMeRepository.getInstance(application)
    private val authManager = AuthManager.getInstance(application)
    private val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private val _activeFilter = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _cloudSyncing = MutableStateFlow(false)
    private val _cloudMessage = MutableStateFlow<String?>("Sincronizado con base de datos local y Firebase")

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
        NotificationHelper.createNotificationChannel(application)
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                repository.switchUser(user?.uid)
            }
        }
    }

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
        val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val currentKey = monthKeyFormat.format(now)
        val monthLabel = monthDisplayFormat.format(now).replaceFirstChar { it.uppercase() }

        // Filter current month transactions for budget & monthly calculation
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

        // Breakdown for expenses
        val expensesCurrentMonth = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }
        val expenseBreakdown = expensesCurrentMonth.groupBy { it.categoryId }
            .map { (catId, items) ->
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

        // Breakdown for income
        val incomesCurrentMonth = currentMonthTransactions.filter { it.type == TransactionType.INCOME }
        val incomeBreakdown = incomesCurrentMonth.groupBy { it.categoryId }
            .map { (catId, items) ->
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

        // Monthly comparison (last 4 months)
        val monthlyComparison = buildMonthlyComparison(transactions)

        // Filtered transaction list for display
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

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            authManager.signInWithGoogle(activity)
        }
    }

    fun authenticateWithFirebaseEmail(email: String, pass: String, name: String) {
        viewModelScope.launch {
            authManager.authenticateWithFirebaseEmail(email, pass, name)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.switchUser("local")
        }
    }

    private fun buildMonthlyComparison(transactions: List<TransactionItem>): List<MonthlyStat> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<MonthlyStat>()
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val shortMonthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))

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

    fun setFilter(type: TransactionType?) {
        _activeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
    }

    fun addOrUpdateTransaction(item: TransactionItem, context: Context) {
        viewModelScope.launch {
            repository.saveTransaction(item)

            // If it's an expense, verify budget and trigger alert if necessary
            if (item.type == TransactionType.EXPENSE) {
                checkAndDispatchBudgetAlert(context, item.categoryName)
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addOrUpdateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.saveGoal(goal)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

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
