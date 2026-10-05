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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uma.contame.model.DefaultCategories
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import com.uma.contame.ui.components.IconHelper
import com.uma.contame.ui.components.TransactionCard
import com.uma.contame.viewmodel.ContaMeUiState
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla para explorar el historial completo de movimientos financieros (transacciones).
 *
 * Ofrece campo de búsqueda en tiempo real por texto/concepto, filtro por tipo (Gastos, Ingresos o Todos)
 * y filtros por chips de categorías.
 *
 * @param state Estado global de la UI ([ContaMeUiState]).
 * @param onFilterChange Callback al cambiar el tipo de filtro (Gasto/Ingreso).
 * @param onSearchChange Callback al ingresar texto en la barra de búsqueda.
 * @param onCategoryFilterChange Callback al seleccionar un chip de categoría.
 * @param onEditTransaction Callback para editar una transacción.
 * @param onDeleteTransaction Callback para eliminar una transacción.
 * @param modifier Modificador de Compose.
 */
@Composable
fun TransactionsScreen(
    state: ContaMeUiState,
    onFilterChange: (TransactionType?) -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-US")) }
    val allCategories = remember { DefaultCategories.expenseCategories + DefaultCategories.incomeCategories }

    val filteredSum = remember(state.filteredTransactions) {
        val inc = state.filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = state.filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        Pair(inc, exp)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barra de Búsqueda
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_input"),
                placeholder = { Text("Buscar gasto, ingreso, categoría o nota...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Filtro por Segmento (Todos / Gastos / Ingresos)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                val tabs = listOf(
                    Triple(null, "Todos", Color.Transparent),
                    Triple(TransactionType.EXPENSE, "Gastos", Color(0xFFEF4444)),
                    Triple(TransactionType.INCOME, "Ingresos", Color(0xFF10B981))
                )

                tabs.forEach { (type, label, activeColor) ->
                    val isSelected = state.activeFilter == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    if (type == null) MaterialTheme.colorScheme.surface else activeColor
                                } else Color.Transparent
                            )
                            .clickable { onFilterChange(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                if (type == null) MaterialTheme.colorScheme.onSurface else Color.White
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filtro por chips de Categoría
        item {
            Column {
                Text(
                    text = "Filtrar por categoría:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryFilter == null,
                            onClick = { onCategoryFilterChange(null) },
                            label = { Text("Todas") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    items(allCategories) { category ->
                        val isSelected = state.selectedCategoryFilter == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onCategoryFilterChange(if (isSelected) null else category.id)
                            },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = IconHelper.getIconByName(category.iconName),
                                    contentDescription = category.name,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Resumen contador del resultado filtrado
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.filteredTransactions.size} movimientos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.activeFilter != TransactionType.EXPENSE && filteredSum.first > 0) {
                            Text(
                                text = "+${currencyFormat.format(filteredSum.first)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        if (state.activeFilter != TransactionType.INCOME && filteredSum.second > 0) {
                            Text(
                                text = "-${currencyFormat.format(filteredSum.second)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        // Lista de transacciones filtradas
        if (state.filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No se encontraron transacciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Intenta cambiar los filtros o registra un nuevo movimiento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.filteredTransactions, key = { it.id }) { item ->
                TransactionCard(
                    item = item,
                    onEditClick = { onEditTransaction(item) },
                    onDeleteClick = { onDeleteTransaction(item.id) }
                )
            }
        }
    }
}
