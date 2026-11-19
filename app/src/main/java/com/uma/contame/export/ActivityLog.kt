package com.uma.contame.export

import android.content.Context
import java.io.File

/**
 * Registro local de acciones del usuario (guardar, editar, eliminar...).
 *
 * Se guarda en un archivo de texto dentro del almacenamiento privado de la app, **sin tocar Room
 * ni Firestore**, por lo que no requiere migraciones de base de datos. Cada línea tiene el formato
 * `timestamp<TAB>acción<TAB>detalle<TAB>monto`. El reporte de Excel lo incluye en la hoja
 * "Registro de acciones".
 */
object ActivityLog {

    private const val FILE_NAME = "activity_log.tsv"
    private const val MAX_LINES = 5000

    /**
     * Acción registrada.
     *
     * @property timestamp Fecha y hora en milisegundos.
     * @property action Nombre corto de la acción (ej: "Movimiento guardado").
     * @property detail Detalle libre (ej: título del movimiento).
     * @property amount Monto asociado, o `null` si no aplica.
     */
    data class Entry(val timestamp: Long, val action: String, val detail: String, val amount: Double?)

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    /** Agrega una acción al final del registro. Nunca lanza excepciones (si falla, se ignora). */
    @Synchronized
    fun record(context: Context, action: String, detail: String = "", amount: Double? = null) {
        runCatching {
            val f = file(context)
            f.appendText("${System.currentTimeMillis()}\t${clean(action)}\t${clean(detail)}\t${amount ?: ""}\n")
            // Si el archivo crece demasiado, conserva solo las últimas MAX_LINES acciones.
            val lines = f.readLines()
            if (lines.size > MAX_LINES) f.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }

    /** Lee todas las acciones registradas, de la más reciente a la más antigua. */
    @Synchronized
    fun readAll(context: Context): List<Entry> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return runCatching {
            f.readLines().mapNotNull { line ->
                val p = line.split('\t')
                val ts = p.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                Entry(ts, p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrNull(3)?.toDoubleOrNull())
            }.sortedByDescending { it.timestamp }
        }.getOrDefault(emptyList())
    }
}
