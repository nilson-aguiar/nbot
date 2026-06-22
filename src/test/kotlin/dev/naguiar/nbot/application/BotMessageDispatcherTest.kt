package dev.naguiar.nbot.application

import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.tools.torrent.TorrentTools
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class BotMessageDispatcherTest {
    private val torrentTools: TorrentTools = mockk()
    private val budgetImportService: BudgetImportService = mockk()
    private val dispatcher = BotMessageDispatcher(torrentTools, budgetImportService)

    @Test
    fun `should process torrent file correctly`() {
        val fileName = "test.torrent"
        val fileContent = "torrent content"
        val inputStream = ByteArrayInputStream(fileContent.toByteArray())
        every { torrentTools.addTorrentFile(any()) } returns "Added successfully"

        val result = dispatcher.processDocument(fileName, inputStream, "http://localhost:8080")

        assertEquals("Added successfully", result)
        verify(exactly = 1) { torrentTools.addTorrentFile(any()) }
    }

    @Test
    fun `should process xml file correctly`() {
        val fileName = "test.xml"
        val fileContent = "xml content"
        val inputStream = ByteArrayInputStream(fileContent.toByteArray())
        every { budgetImportService.importCamt(any()) } returns "success"

        val result = dispatcher.processDocument(fileName, inputStream, "http://localhost:8080")

        assertEquals(
            "Successfully imported transactions. Review them on the dashboard: http://localhost:8080/dashboard",
            result,
        )
        verify(exactly = 1) { budgetImportService.importCamt(any()) }
    }

    @Test
    fun `should return error message for unsupported file types`() {
        val fileName = "test.txt"
        val fileContent = "text content"
        val inputStream = ByteArrayInputStream(fileContent.toByteArray())

        val result = dispatcher.processDocument(fileName, inputStream, "http://localhost:8080")

        assertEquals("I only understand .torrent and .xml files at the moment.", result)
    }
}
