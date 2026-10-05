package com.uma.contame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uma.contame.model.TransactionType
import com.uma.contame.ui.components.ContaMeDonutChart
import com.uma.contame.ui.components.ContaMeMonthlyBarChart
import com.uma.contame.viewmodel.ContaMeUiState
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla que presenta estadísticas financieras, gráficos de dona interactivos por categoría,
 * gráficos de barras para comparar Ingresos vs. Gastos y métricas clave (KPIs).
 *
 * @param state Estado global de la UI ([ContaMeUiState]).
 * @param modifier Modificador de Compose.
 */
@Composable
fun ChartsScreen(
    state: ContaMeUiState,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-US")) }

    val activeBreakdown = if (chartType == TransactionType.EXPENSE) {
        state.expenseCategoriesBreakdown
    } else {
        state.incomeCategoriesBreakdown
    }

    val activeTotal = if (chartType == TransactionType.EXPENSE) {
        state.monthlyExpense
    } else {
        state.monthlyIncome
    }

    // Cálculo del porcentaje/tasa de ahorro
    val savingsRate = remember(state.monthlyIncome, state.monthlyExpense) {
        if (state.monthlyIncome > 0) {
            val saved = state.monthlyIncome - state.monthlyExpense
            ((saved / state.monthlyIncome) * 100).toInt()
        } else 0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("charts_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de la sección
        item {
            Column {
                Text(
                    text = "Gráficos y Estadísticas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Análisis visual de tus finanzas en ${state.selectedMonthLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selector: Gastos por Categoría vs Ingresos por Categoría
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                val isExpense = chartType == TransactionType.EXPENSE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExpense) Color(0xFFEF4444) else Color.Transparent)
                        .clickable { chartType = TransactionType.EXPENSE }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gastos por Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isExpense) Color(0xFF10B981) else Color.Transparent)
                        .clickable { chartType = TransactionType.INCOME }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ingresos por Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Gráfico de Dona
        item {
            ContaMeDonutChart(
                breakdown = activeBreakdown,
                totalAmount = activeTotal,
                title = if (chartType == TransactionType.EXPENSE) "Distribución de Gastos" else "Fuentes de Ingreso"
            )
        }

        // Gráfico de Barras
        item {
            ContaMeMonthlyBarChart(
                stats = state.monthlyComparison
            )
        }

        // Tarjetas de Métricas e Indicadores
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tasa de ahorro
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2210B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tasa Ahorro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$savingsRate%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (savingsRate >= 20) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        Text(
                            text = if (savingsRate >= 20) "Excelente ritmo de ahorro" else "Margen de ahorro ajustado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Categoría de mayor gasto
                val topExpense = state.expenseCategoriesBreakdown.firstOrNull()
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22EF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mayor Gasto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = topExpense?.categoryName ?: "Ninguno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (topExpense != null) currencyFormat.format(topExpense.totalAmount) else "$0.00",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
