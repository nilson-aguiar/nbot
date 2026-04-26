package dev.naguiar.nbot.presentation.telegram

import dev.naguiar.nbot.application.ChatOrchestrator
import dev.naguiar.nbot.infrastructure.config.SecurityProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class NbotTelegramObserver(
    private val securityProperties: SecurityProperties,
    private val chatOrchestrator: ChatOrchestrator,
) : TelegramLongPollingBot(securityProperties.telegramBotToken) {
    private val log = LoggerFactory.getLogger(NbotTelegramObserver::class.java)

    override fun getBotUsername(): String = "Nbot"

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage()) return
        val message = update.message
        val userId = message.from.id

        if (!securityProperties.allowedUsers.contains(userId)) {
            log.warn("Unauthorized access attempt from user ID: {}", userId)
            return
        }

        if (message.hasText()) {
            val responseText = chatOrchestrator.chat(message.text)
            sendReply(message.chatId, responseText)
        } else if (message.hasDocument()) {
            val document = message.document
            if (document.fileName?.endsWith(".torrent", ignoreCase = true) == true) {
                try {
                    val getFile = org.telegram.telegrambots.meta.api.methods.GetFile()
                    getFile.fileId = document.fileId
                    val telegramFile = execute(getFile)
                    val downloadedFile = downloadFile(telegramFile)
                    
                    val prompt = """
                        System Context: The user forwarded a .torrent file.
                        File Name: ${document.fileName}
                        File Path: ${downloadedFile.absolutePath}
                        User Caption: ${message.caption ?: "None"}
                        
                        Please handle this intent by using the addTorrentFile tool.
                    """.trimIndent()
                    
                    val responseText = chatOrchestrator.chat(prompt)
                    sendReply(message.chatId, responseText)
                } catch (e: Exception) {
                    log.error("Failed to process document", e)
                    sendReply(message.chatId, "Failed to download and process the torrent file.")
                }
            } else {
                 sendReply(message.chatId, "I only understand .torrent files at the moment.")
            }
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
