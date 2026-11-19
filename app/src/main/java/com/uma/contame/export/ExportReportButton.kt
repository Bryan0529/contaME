package com.uma.contame.export

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uma.contame.viewmodel.ContaMeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Botón de la barra superior que abre un diálogo con tres opciones: compartir el resumen por
 * WhatsApp, enviar el Excel por correo o por WhatsApp.
 *
 * Es **autónomo**: guarda su propio estado interno, así que no requiere variables nuevas en
 * `ContaMeApp`. Solo se coloca dentro de las `actions` de la `TopAppBar`.
 *
 * @param state Estado actual de la app (se lee, nunca se modifica).
 */
@Composable
fun ExportReportButton(state: ContaMeUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    /** Genera el Excel en segundo plano y luego ejecuta [send] en el hilo principal. */
    fun withExcel(send: (File) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { ReportBuilder.buildExcelFile(context, state) }
                open = false
                send(file)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo generar el reporte: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Default.Share, contentDescription = "Exportar y compartir reporte")
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { if (!busy) open = false },
            title = { Text("Exportar reporte", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Reporte de ${state.selectedMonthLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExportOption(
                        icon = Icons.Default.Chat,
                        tint = Color(0xFF25D366),
                        title = "Resumen por WhatsApp",
                        subtitle = "Mensaje ordenado con tus números del mes",
                        enabled = !busy
                    ) {
                        ReportShare.shareTextToWhatsApp(context, ReportBuilder.buildWhatsAppText(state))
                        open = false
                    }
                    ExportOption(
                        icon = Icons.Default.Image,
                        tint = Color(0xFFF59E0B),
                        title = "Tarjeta como imagen",
                        subtitle = "Imagen con el diseño de contaME para WhatsApp",
                        enabled = !busy
                    ) {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                try {
                                    val img = withContext(Dispatchers.IO) { ShareImageCard.render(context, state) }
                                    open = false
                                    ReportShare.shareImageToWhatsApp(context, img, "Mi resumen de ${state.selectedMonthLabel} en contaME")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo crear la imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                    ExportOption(
                        icon = Icons.Default.Email,
                        tint = Color(0xFF3B82F6),
                        title = "Excel por correo",
                        subtitle = "Adjunta el reporte completo (.xlsx)",
                        enabled = !busy
                    ) {
                        withExcel { file ->
                            ReportShare.sendEmail(
                                context, file,
                                subject = "Reporte contaME – ${state.selectedMonthLabel}",
                                body = ReportBuilder.buildEmailBody(state)
                            )
                        }
                    }
                    ExportOption(
                        icon = Icons.Default.TableChart,
                        tint = Color(0xFF10B981),
                        title = "Excel por WhatsApp",
                        subtitle = "Envía el archivo .xlsx como documento",
                        enabled = !busy
                    ) {
                        withExcel { file ->
                            ReportShare.shareFileToWhatsApp(context, file, "Reporte contaME – ${state.selectedMonthLabel}")
                        }
                    }
                    if (busy) {
                        Text(
                            text = "Generando archivo…",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }, enabled = !busy) { Text("Cerrar") } }
        )
    }
}

/** Fila de opción con icono de color, título y descripción corta (botón grande y claro). */
@Composable
private fun ExportOption(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
