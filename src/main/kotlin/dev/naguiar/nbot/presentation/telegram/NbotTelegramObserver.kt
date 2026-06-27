package dev.naguiar.nbot.presentation.telegram

import dev.naguiar.nbot.application.BotMessageDispatcher
import dev.naguiar.nbot.infrastructure.config.TelegramProperties
import java.io.FileInputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class NbotTelegramObserver(
    private val telegramProperties: TelegramProperties,
    private val botMessageDispatcher: BotMessageDispatcher,
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
            processDocument(document, message, fileName)
            return
        }

        sendReply(message.chatId, "Default answer: I cannot handle your message.")
    }

    private fun processDocument(
        document: Document,
        message: Message,
        fileName: String,
    ) {
        try {
            val getFile = GetFile()
            getFile.fileId = document.fileId
            val telegramFile = execute(getFile)
            val downloadedFile = downloadFile(telegramFile)

            val replyText =
                FileInputStream(downloadedFile).use { inputStream ->
                    botMessageDispatcher.processDocument(
                        fileName = fileName,
                        inputStream = inputStream,
                        dashboardUrl = telegramProperties.dashboardUrl,
                    )
                }
            sendReply(message.chatId, replyText)
        } catch (e: Exception) {
            log.error("Failed to process document", e)
            sendReply(message.chatId, "Failed to download and process the file.")
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
