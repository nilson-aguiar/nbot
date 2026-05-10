package dev.naguiar.nbot.presentation.telegram

import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.infrastructure.config.TelegramProperties
import dev.naguiar.nbot.tools.torrent.TorrentTools
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals

class NbotTelegramObserverTest {

    private val telegramProperties = mockk<TelegramProperties>()
    private val torrentTools = mockk<TorrentTools>()
    private val budgetImportService = mockk<BudgetImportService>()
    private lateinit var observer: NbotTelegramObserver

    @BeforeEach
    fun setUp() {
        every { telegramProperties.telegramBotToken } returns "fake-token"
        observer = spyk(NbotTelegramObserver(telegramProperties, torrentTools, budgetImportService))
    }

    @Test
    fun `should return user ID when ID command is received`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 12345L
        every { message.chatId } returns 67890L
        every { message.hasText() } returns true
        every { message.text } returns "/id"

        val sendMessageSlot = slot<SendMessage>()
        every { observer.execute(capture(sendMessageSlot)) } returns mockk()

        observer.onUpdateReceived(update)

        assertEquals("67890", sendMessageSlot.captured.chatId)
        assertEquals("Your Telegram User ID is: 12345", sendMessageSlot.captured.text)
    }

    @Test
    fun `should not process messages from unauthorized users`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 999L // Unauthorized
        every { message.chatId } returns 67890L
        every { message.hasText() } returns true
        every { message.text } returns "Hello"
        every { telegramProperties.allowedUsers } returns listOf(1L, 2L)

        observer.onUpdateReceived(update)

        verify(exactly = 0) { observer.execute(any<SendMessage>()) }
    }

    @Test
    fun `should return default answer for authorized users with plain text`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 1L
        every { message.chatId } returns 67890L
        every { message.hasText() } returns true
        every { message.text } returns "Hello"
        every { message.hasDocument() } returns false
        every { telegramProperties.allowedUsers } returns listOf(1L)

        val sendMessageSlot = slot<SendMessage>()
        every { observer.execute(capture(sendMessageSlot)) } returns mockk()

        observer.onUpdateReceived(update)

        assertEquals("Default answer: I cannot handle your message.", sendMessageSlot.captured.text)
    }

    @Test
    fun `should process torrent document from authorized user`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()
        val document = mockk<Document>()
        val telegramFile = mockk<org.telegram.telegrambots.meta.api.objects.File>()
        val tempFile = File.createTempFile("test", ".torrent")

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 1L
        every { message.chatId } returns 67890L
        every { message.hasText() } returns false
        every { message.hasDocument() } returns true
        every { message.document } returns document
        every { document.fileName } returns "test.torrent"
        every { document.fileId } returns "file123"
        every { telegramProperties.allowedUsers } returns listOf(1L)

        every { observer.execute(any<GetFile>()) } returns telegramFile
        every { observer.downloadFile(telegramFile) } returns tempFile
        every { torrentTools.addTorrentFile(any()) } returns "Torrent added successfully"
        
        val sendMessageSlot = slot<SendMessage>()
        every { observer.execute(capture(sendMessageSlot)) } returns mockk()

        observer.onUpdateReceived(update)

        assertEquals("Torrent added successfully", sendMessageSlot.captured.text)
        
        tempFile.delete()
    }

    @Test
    fun `should process CAMT XML document from authorized user`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()
        val document = mockk<Document>()
        val telegramFile = mockk<org.telegram.telegrambots.meta.api.objects.File>()
        val tempFile = File.createTempFile("test", ".xml")

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 1L
        every { message.chatId } returns 67890L
        every { message.hasText() } returns false
        every { message.hasDocument() } returns true
        every { message.document } returns document
        every { document.fileName } returns "test.xml"
        every { document.fileId } returns "file123"
        every { telegramProperties.allowedUsers } returns listOf(1L)
        every { telegramProperties.dashboardUrl } returns "http://localhost:8080"

        every { observer.execute(any<GetFile>()) } returns telegramFile
        every { observer.downloadFile(telegramFile) } returns tempFile
        every { budgetImportService.importCamt(any<InputStream>()) } returns "export-123"

        val sendMessageSlot = slot<SendMessage>()
        every { observer.execute(capture(sendMessageSlot)) } returns mockk()

        observer.onUpdateReceived(update)

        assertEquals("Successfully imported transactions. Review them on the dashboard: http://localhost:8080/dashboard", sendMessageSlot.captured.text)

        tempFile.delete()
    }

    @Test
    fun `should handle errors during torrent processing`() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        val user = mockk<User>()
        val document = mockk<Document>()

        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.from } returns user
        every { user.id } returns 1L
        every { message.chatId } returns 67890L
        every { message.hasText() } returns false
        every { message.hasDocument() } returns true
        every { message.document } returns document
        every { document.fileName } returns "test.torrent"
        every { document.fileId } returns "file123"
        every { telegramProperties.allowedUsers } returns listOf(1L)

        every { observer.execute(any<GetFile>()) } throws RuntimeException("Download failed")
        
        val sendMessageSlot = slot<SendMessage>()
        every { observer.execute(capture(sendMessageSlot)) } returns mockk()

        observer.onUpdateReceived(update)

        assertEquals("Failed to download and process the torrent file.", sendMessageSlot.captured.text)
    }
}
