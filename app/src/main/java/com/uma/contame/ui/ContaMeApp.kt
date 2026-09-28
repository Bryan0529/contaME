package com.uma.contame.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.uma.contame.R
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import com.uma.contame.ui.components.AddEditGoalDialog
import com.uma.contame.ui.components.AddEditTransactionDialog
import com.uma.contame.ui.components.ContaMeIcon
import com.uma.contame.ui.components.DepositGoalDialog
import com.uma.contame.ui.components.GoogleLogoIcon
import com.uma.contame.ui.components.SetBudgetDialog
import com.uma.contame.ui.components.UserAccountDialog
import com.uma.contame.ui.screens.BudgetScreen
import com.uma.contame.ui.screens.ChartsScreen
import com.uma.contame.ui.screens.DashboardScreen
import com.uma.contame.ui.screens.LoginScreen
import com.uma.contame.ui.screens.SavingsGoalsScreen
import com.uma.contame.ui.screens.TransactionsScreen
import com.uma.contame.viewmodel.ContaMeViewModel

enum class AppTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Resumen", Icons.Default.Home),
    TRANSACTIONS("Movimientos", Icons.Default.ReceiptLong),
    CHARTS("Gráficos", Icons.Default.BarChart),
    GOALS("Metas", Icons.Default.Savings),
    BUDGET("Presupuesto", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContaMeApp(
    viewModel: ContaMeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Dialog states
    var showTxDialog by remember { mutableStateOf(false) }
    var editingTxItem by remember { mutableStateOf<TransactionItem?>(null) }
    var presetTxType by remember { mutableStateOf<TransactionType?>(null) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoalItem by remember { mutableStateOf<SavingsGoal?>(null) }

    var showDepositDialog by remember { mutableStateOf(false) }
    var depositingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    var txToDelete by remember { mutableStateOf<String?>(null) }
    var goalToDelete by remember { mutableStateOf<String?>(null) }

    // Handle back button: if not on DASHBOARD, return to DASHBOARD
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    // Permission request for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Si no ha iniciado sesión, mostrar la pantalla de Login con el botón de Google
    if (uiState.userProfile == null) {
        LoginScreen(
            isLoading = uiState.isAuthLoading,
            errorMessage = uiState.authError,
            onGoogleSignInClick = {
                if (activity != null) {
                    viewModel.signInWithGoogle(activity)
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        ContaMeIcon(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "conta",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "ME",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            Text(
                                text = "Gastos, Ingresos y Ahorro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isBudgetExceeded || uiState.isBudgetWarning) {
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.isBudgetExceeded) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alerta activa",
                                    tint = if (uiState.isBudgetExceeded) Color(0xFFDC2626) else Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isBudgetExceeded) "Excedido" else "Alerta",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isBudgetExceeded) Color(0xFFDC2626) else Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Google Account avatar with proprietor tooltip/click
                    val profile = uiState.userProfile!!
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF))
                            .clickable { showAccountDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile.photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = profile.photoUrl,
                                contentDescription = "Perfil de Google",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (profile.displayName ?: profile.email ?: "U").take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != AppTab.BUDGET) {
                FloatingActionButton(
                    onClick = {
                        editingTxItem = null
                        presetTxType = null
                        showTxDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("main_fab_add")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo movimiento")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        state = uiState,
                        onAddTransaction = { type ->
                            editingTxItem = null
                            presetTxType = type
                            showTxDialog = true
                        },
                        onAddGoal = {
                            editingGoalItem = null
                            showGoalDialog = true
                        },
                        onEditTransaction = { item ->
                            editingTxItem = item
                            showTxDialog = true
                        },
                        onDeleteTransaction = { id ->
                            txToDelete = id
                        },
                        onAddFundsGoal = { goal ->
                            depositingGoal = goal
                            showDepositDialog = true
                        },
                        onEditGoal = { goal ->
                            editingGoalItem = goal
                            showGoalDialog = true
                        },
                        onDeleteGoal = { id ->
                            goalToDelete = id
                        },
                        onConfigureBudget = {
                            showBudgetDialog = true
                        },
                        onViewAllTransactions = {
                            currentTab = AppTab.TRANSACTIONS
                        },
                        onViewAllGoals = {
                            currentTab = AppTab.GOALS
                        }
                    )

                    AppTab.TRANSACTIONS -> TransactionsScreen(
                        state = uiState,
                        onFilterChange = { viewModel.setFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onEditTransaction = { item ->
                            editingTxItem = item
                            showTxDialog = true
                        },
                        onDeleteTransaction = { id ->
                            txToDelete = id
                        }
                    )

                    AppTab.CHARTS -> ChartsScreen(state = uiState)

                    AppTab.GOALS -> SavingsGoalsScreen(
                        state = uiState,
                        onAddGoal = {
                            editingGoalItem = null
                            showGoalDialog = true
                        },
                        onAddFunds = { goal ->
                            depositingGoal = goal
                            showDepositDialog = true
                        },
                        onEditGoal = { goal ->
                            editingGoalItem = goal
                            showGoalDialog = true
                        },
                        onDeleteGoal = { id ->
                            goalToDelete = id
                        }
                    )

                    AppTab.BUDGET -> BudgetScreen(
                        state = uiState,
                        onConfigureBudget = {
                            showBudgetDialog = true
                        },
                        onToggleNotifications = { enabled ->
                            viewModel.updateMonthlyBudget(
                                limit = uiState.monthlyBudget.budgetLimit,
                                thresholdPercent = uiState.monthlyBudget.alertThresholdPercent,
                                notificationsEnabled = enabled
                            )
                        },
                        onSyncCloud = {
                            viewModel.syncWithCloud()
                        },
                        onGoogleSignIn = {
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity)
                            }
                        },
                        onSignOut = {
                            viewModel.signOut()
                        }
                    )
                }
            }
        }
    }

    // Account Dialog
    if (showAccountDialog && uiState.userProfile != null) {
        UserAccountDialog(
            user = uiState.userProfile!!,
            onSignOut = {
                viewModel.signOut()
                showAccountDialog = false
            },
            onSwitchAccount = {
                viewModel.signOut()
                showAccountDialog = false
            },
            onDismiss = { showAccountDialog = false }
        )
    }

    // Modal Dialogs
    if (showTxDialog) {
        val initialWithPreset = editingTxItem ?: presetTxType?.let {
            TransactionItem(
                title = "",
                amount = 0.0,
                type = it,
                categoryId = if (it == TransactionType.EXPENSE) "exp_food" else "inc_salary",
                categoryName = if (it == TransactionType.EXPENSE) "Alimentación" else "Salario",
                categoryIcon = if (it == TransactionType.EXPENSE) "restaurant" else "payments",
                categoryColor = if (it == TransactionType.EXPENSE) "#EF4444" else "#10B981"
            )
        }

        AddEditTransactionDialog(
            initialItem = initialWithPreset,
            onDismiss = {
                showTxDialog = false
                editingTxItem = null
                presetTxType = null
            },
            onSave = { item ->
                viewModel.addOrUpdateTransaction(item, context)
                showTxDialog = false
                editingTxItem = null
                presetTxType = null
            }
        )
    }

    if (showGoalDialog) {
        AddEditGoalDialog(
            initialGoal = editingGoalItem,
            onDismiss = {
                showGoalDialog = false
                editingGoalItem = null
            },
            onSave = { goal ->
                viewModel.addOrUpdateGoal(goal)
                showGoalDialog = false
                editingGoalItem = null
            }
        )
    }

    if (showDepositDialog && depositingGoal != null) {
        DepositGoalDialog(
            goal = depositingGoal!!,
            onDismiss = {
                showDepositDialog = false
                depositingGoal = null
            },
            onDeposit = { amount, registerExpense ->
                viewModel.addFundsToGoal(depositingGoal!!, amount, registerExpense, context)
                showDepositDialog = false
                depositingGoal = null
            }
        )
    }

    if (showBudgetDialog) {
        SetBudgetDialog(
            currentBudget = uiState.monthlyBudget,
            onDismiss = { showBudgetDialog = false },
            onSave = { limit, threshold, notificationsEnabled ->
                viewModel.updateMonthlyBudget(limit, threshold, notificationsEnabled)
                showBudgetDialog = false
            }
        )
    }

    // Delete Transaction confirmation dialog
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Eliminar Registro") },
            text = { Text("¿Estás seguro de que deseas eliminar este movimiento? Esta acción también se sincronizará con Firebase.") },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete?.let { viewModel.deleteTransaction(it) }
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Goal confirmation dialog
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Eliminar Meta de Ahorro") },
            text = { Text("¿Deseas eliminar esta meta de ahorro? Los fondos ahorrados registrados se removerán de la meta.") },
            confirmButton = {
                Button(
                    onClick = {
                        goalToDelete?.let { viewModel.deleteGoal(it) }
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
