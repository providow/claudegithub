package com.gcashagent.tracker

import com.gcashagent.tracker.core.util.ExcelWriter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class ExcelWriterTest {

    @Test
    fun producesValidZipWithWellFormedXmlParts() {
        val file = File.createTempFile("report", ".xlsx")
        file.deleteOnExit()

        ExcelWriter.write(
            file,
            listOf(
                ExcelWriter.Sheet(
                    name = "Report",
                    rows = listOf(
                        listOf(ExcelWriter.cell("Label"), ExcelWriter.cell("Amount")),
                        listOf(ExcelWriter.cell("Cash In <test> & \"quotes\""), ExcelWriter.cell(1234.50)),
                        listOf(ExcelWriter.cell("Net"), ExcelWriter.cell(-500.0))
                    )
                )
            )
        )

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        ZipFile(file).use { zip ->
            val required = listOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml"
            )
            for (name in required) {
                val entry = zip.getEntry(name)
                assertNotNull("missing part: $name", entry)
                // Each part must be well-formed XML (this also verifies escaping).
                zip.getInputStream(entry).use { builder.parse(it) }
            }
        }
    }

    @Test
    fun multipleSheetsAreAllWritten() {
        val file = File.createTempFile("multi", ".xlsx")
        file.deleteOnExit()
        ExcelWriter.write(
            file,
            listOf(
                ExcelWriter.Sheet("One", listOf(listOf(ExcelWriter.cell("a")))),
                ExcelWriter.Sheet("Two", listOf(listOf(ExcelWriter.cell("b"))))
            )
        )
        ZipFile(file).use { zip ->
            assertTrue(zip.getEntry("xl/worksheets/sheet1.xml") != null)
            assertTrue(zip.getEntry("xl/worksheets/sheet2.xml") != null)
        }
    }
}
