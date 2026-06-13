package com.gcashagent.tracker.core.util

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A tiny, dependency-free writer for the OOXML (.xlsx) Spreadsheet format.
 *
 * It produces a valid multi-sheet workbook by zipping a minimal set of XML
 * parts and uses inline strings (no shared-strings table) to keep the code
 * small. Numbers are written as numeric cells so Excel/Sheets treat them as
 * real values. Fully offline — no native libraries.
 */
object ExcelWriter {

    sealed interface Cell {
        data class Text(val value: String) : Cell
        data class Number(val value: Double) : Cell
        data object Empty : Cell
    }

    data class Sheet(val name: String, val rows: List<List<Cell>>)

    fun cell(text: String): Cell = Cell.Text(text)
    fun cell(number: Double): Cell = Cell.Number(number)

    fun write(file: File, sheets: List<Sheet>) {
        file.parentFile?.mkdirs()
        file.outputStream().use { write(it, sheets) }
    }

    fun write(out: OutputStream, sheets: List<Sheet>) {
        require(sheets.isNotEmpty()) { "A workbook needs at least one sheet." }
        ZipOutputStream(out).use { zip ->
            zip.putString("[Content_Types].xml", contentTypes(sheets.size))
            zip.putString("_rels/.rels", rootRels())
            zip.putString("xl/workbook.xml", workbook(sheets))
            zip.putString("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.putString("xl/styles.xml", styles())
            sheets.forEachIndexed { index, sheet ->
                zip.putString("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
    }

    private fun ZipOutputStream.putString(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun rootRels(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private fun workbook(sheets: List<Sheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("<sheets>")
        sheets.forEachIndexed { index, sheet ->
            val id = index + 1
            append("""<sheet name="${escape(sheetName(sheet.name))}" sheetId="$id" r:id="rId$id"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        // styles part gets the next free relationship id
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    /** Minimal styles part (a single default cell format) so consumers stay happy. */
    private fun styles(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="1"><fill><patternFill patternType="none"/></fill></fills>""" +
            """<borders count="1"><border/></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>""" +
            """</styleSheet>"""

    private fun sheetXml(sheet: Sheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("<sheetData>")
        sheet.rows.forEachIndexed { rowIndex, cells ->
            val rowNum = rowIndex + 1
            append("""<row r="$rowNum">""")
            cells.forEachIndexed { colIndex, cell ->
                val ref = columnLetter(colIndex) + rowNum
                when (cell) {
                    is Cell.Text ->
                        append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${escape(cell.value)}</t></is></c>""")
                    is Cell.Number ->
                        append("""<c r="$ref"><v>${numberLiteral(cell.value)}</v></c>""")
                    Cell.Empty -> { /* skip empty cells */ }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun numberLiteral(value: Double): String {
        // Avoid scientific notation / locale separators in the literal.
        return if (value == value.toLong().toDouble()) value.toLong().toString()
        else java.math.BigDecimal(value).stripTrailingZeros().toPlainString()
    }

    /** 0 -> "A", 25 -> "Z", 26 -> "AA". */
    private fun columnLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
        }
        return sb.toString()
    }

    /** Sheet names are limited to 31 chars and may not contain : \ / ? * [ ] */
    private fun sheetName(raw: String): String {
        val sanitized = raw.replace(Regex("[:\\\\/?*\\[\\]]"), " ").trim()
        val safe = sanitized.ifEmpty { "Sheet" }
        return if (safe.length > 31) safe.substring(0, 31) else safe
    }

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
