package dev.naguiar.nbot.application

import dev.naguiar.nbot.tools.torrent.TorrentTools
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BotMessageDispatcherTest {
    private val torrentTools: TorrentTools = mockk()
    private val dispatcher = BotMessageDispatcher(torrentTools)

    @Test
    fun `should process torrent file correctly`() {
        val fileName = "test.torrent"
        val fileContent = "torrent content"
        val inputStream = ByteArrayInputStream(fileContent.toByteArray())
        every { torrentTools.addTorrentFile(any()) } returns "Added successfully"

        val result = dispatcher.processDocument(fileName, inputStream)

        assertEquals("Added successfully", result)
        verify(exactly = 1) { torrentTools.addTorrentFile(any()) }
    }

    @Test
    fun `should return error message for unsupported file types`() {
        val fileName = "test.txt"
        val fileContent = "text content"
        val inputStream = ByteArrayInputStream(fileContent.toByteArray())

        val result = dispatcher.processDocument(fileName, inputStream)

        assertEquals("I only understand .torrent files at the moment.", result)
    }
}
