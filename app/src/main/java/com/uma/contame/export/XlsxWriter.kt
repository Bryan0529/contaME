package com.uma.contame.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Celda de una hoja de Excel.
 *
 * @property value Valor de la celda: [String], [Number] o `null` (celda vacía).
 * @property style Estilo: [XlsxSheet.STYLE_NORMAL], [XlsxSheet.STYLE_HEADER],
 * [XlsxSheet.STYLE_MONEY], [XlsxSheet.STYLE_BOLD] o [XlsxSheet.STYLE_PERCENT].
 */
data class XlsxCell(val value: Any?, val style: Int = XlsxSheet.STYLE_NORMAL)

/**
 * Hoja de un libro de Excel. Se llena fila por fila con [addRow] o [addHeader].
 *
 * @property name Nombre de la pestaña (se limpia automáticamente: máx. 31 caracteres, sin `: \ / ? * [ ]`).
 */
class XlsxSheet(name: String) {

    /** Nombre de pestaña válido para Excel. */
    val name: String = name.replace(Regex("[:\\\\/?*\\[\\]]"), " ").trim().take(31).ifEmpty { "Hoja" }

    internal val rows = mutableListOf<List<XlsxCell>>()

    /** Anchos de columna en caracteres (índice 0 = columna A). */
    var columnWidths: List<Double> = emptyList()

    /** Agrega una fila de encabezado (negrita, fondo verde). */
    fun addHeader(vararg titles: String) {
        rows.add(titles.map { XlsxCell(it, STYLE_HEADER) })
    }

    /** Agrega una fila con celdas ya estilizadas. */
    fun addRow(vararg cells: XlsxCell) {
        rows.add(cells.toList())
    }

    /** Agrega una fila de valores con estilo normal. */
    fun addValues(vararg values: Any?) {
        rows.add(values.map { XlsxCell(it) })
    }

    /** Agrega una fila vacía como separador. */
    fun addBlank() {
        rows.add(emptyList())
    }

    companion object {
        const val STYLE_NORMAL = 0
        const val STYLE_HEADER = 1
        const val STYLE_MONEY = 2
        const val STYLE_BOLD = 3
        const val STYLE_PERCENT = 4
    }
}

/**
 * Generador mínimo de archivos `.xlsx` (Office Open XML) **sin librerías externas**.
 *
 * Un `.xlsx` es un ZIP con archivos XML. Este escritor crea solo lo necesario para que Excel,
 * Google Sheets y LibreOffice abran el archivo con varias hojas, encabezados con color y formato
 * de moneda. Se evita Apache POI porque agrega varios MB y es lento en Android.
 */
object XlsxWriter {

    /**
     * Escribe el libro completo en [file].
     *
     * @param file Archivo de destino (se sobrescribe).
     * @param sheets Hojas del libro, en orden. Debe haber al menos una.
     */
    fun write(file: File, sheets: List<XlsxSheet>) {
        require(sheets.isNotEmpty()) { "El libro necesita al menos una hoja" }
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            fun put(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put("_rels/.rels", ROOT_RELS)
            put("xl/workbook.xml", workbook(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, sheet -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(sheet)) }
        }
    }

    private const val XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"

    private fun contentTypes(n: Int): String = buildString {
        append(XML_HEAD)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        for (i in 1..n) {
            append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private const val ROOT_RELS = XML_HEAD +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private fun workbook(sheets: List<XlsxSheet>): String = buildString {
        append(XML_HEAD)
        append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>")
        sheets.forEachIndexed { i, s ->
            append("<sheet name=\"${esc(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(n: Int): String = buildString {
        append(XML_HEAD)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        for (i in 1..n) {
            append("<Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        append("<Relationship Id=\"rId${n + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    /**
     * Estilos (índices usados por [XlsxSheet]): 0 normal, 1 encabezado, 2 moneda, 3 negrita, 4 porcentaje.
     */
    private const val STYLES = XML_HEAD +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;\$&quot;#,##0.00\"/></numFmts>" +
        "<fonts count=\"3\">" +
        "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "</fonts>" +
        "<fills count=\"3\">" +
        "<fill><patternFill patternType=\"none\"/></fill>" +
        "<fill><patternFill patternType=\"gray125\"/></fill>" +
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF10B981\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
        "</fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"5\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "</cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
        "</styleSheet>"

    private fun sheetXml(sheet: XlsxSheet): String = buildString {
        append(XML_HEAD)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        if (sheet.columnWidths.isNotEmpty()) {
            append("<cols>")
            sheet.columnWidths.forEachIndexed { i, w ->
                append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }
        append("<sheetData>")
        sheet.rows.forEachIndexed { r, cells ->
            append("<row r=\"${r + 1}\">")
            cells.forEachIndexed { c, cell ->
                val ref = "${columnLetter(c)}${r + 1}"
                when (val v = cell.value) {
                    null -> append("<c r=\"$ref\" s=\"${cell.style}\"/>")
                    is Number -> append("<c r=\"$ref\" s=\"${cell.style}\"><v>${v.toDouble()}</v></c>")
                    else -> append("<c r=\"$ref\" s=\"${cell.style}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v.toString())}</t></is></c>")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** Convierte un índice de columna (0 = A, 25 = Z, 26 = AA...) a su letra de Excel. */
    internal fun columnLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    /** Escapa caracteres especiales de XML y elimina caracteres de control no válidos. */
    private fun esc(s: String): String = buildString {
        for (ch in s) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch.code < 0x20 && ch != '\n' && ch != '\t' && ch != '\r' -> Unit
                else -> append(ch)
            }
        }
    }
}
