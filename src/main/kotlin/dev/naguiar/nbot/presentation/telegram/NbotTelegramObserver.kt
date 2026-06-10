package dev.naguiar.nbot.presentation.telegram

import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.infrastructure.config.TelegramProperties
import dev.naguiar.nbot.tools.torrent.TorrentTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.io.FileInputStream

@Component
class NbotTelegramObserver(
    private val telegramProperties: TelegramProperties,
    private val torrentTools: TorrentTools,
    private val budgetImportService: BudgetImportService,
) : TelegramLongPollingBot(telegramProperties.telegramBotToken) {
    private val log = LoggerFactory.getLogger(NbotTelegramObserver::class.java)

    override fun getBotUsername(): String = "naguiarBot"

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage()) return
        val message = update.message
        val userId = message.from.id

        if (message.hasText() && message.text.trim() == "/id") {
            sendReply(message.chatId, "Your Telegram User ID is: $userId")
            return
        }

        if (!telegramProperties.allowedUsers.contains(userId)) {
            log.warn("Unauthorized access attempt from user ID: {}", userId)
            return
        }

        if (message.hasDocument()) {
            val document = message.document
            val fileName = document.fileName ?: ""
            if (fileName.endsWith(".torrent", ignoreCase = true)) {
                processTorrentDocument(document, message)
            } else if (fileName.endsWith(".xml", ignoreCase = true)) {
                processCamtDocument(document, message)
            } else {
                sendReply(message.chatId, "I only understand .torrent and .xml files at the moment.")
            }
            return
        }

        sendReply(message.chatId, "Default answer: I cannot handle your message.")
    }

    private fun processTorrentDocument(
        document: Document,
        message: Message,
    ) {
        try {
            val getFile = GetFile()
            getFile.fileId = document.fileId
            val tempFile = File.createTempFile("${document.fileName}", ".torrent")
            val telegramFile = execute(getFile)
            val downloadedFile = downloadFile(telegramFile)
            downloadedFile.renameTo(tempFile)

            // Call the tool directly to bypass AI safety filters
            val responseText = torrentTools.addTorrentFile(tempFile.absolutePath)
            sendReply(message.chatId, responseText)
        } catch (e: Exception) {
            log.error("Failed to process document", e)
            sendReply(message.chatId, "Failed to download and process the torrent file.")
        }
    }

    private fun processCamtDocument(
        document: Document,
        message: Message,
    ) {
        try {
            val getFile = GetFile()
            getFile.fileId = document.fileId
            val telegramFile = execute(getFile)
            val downloadedFile = downloadFile(telegramFile)

            FileInputStream(downloadedFile).use { inputStream ->
                budgetImportService.importCamt(inputStream)
            }

            sendReply(
                message.chatId,
                "Successfully imported transactions. Review them on the dashboard: ${telegramProperties.dashboardUrl}/dashboard",
            )
        } catch (e: Exception) {
            log.error("Failed to process CAMT document", e)
            sendReply(message.chatId, "Failed to download and process the CAMT file.")
        }
    }

    private fun sendReply(
        chatId: Long,
        text: String,
    ) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = text
        try {
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            log.error("Failed to send reply", e)
        }
    }
}
