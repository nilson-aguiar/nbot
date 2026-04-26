package dev.naguiar.nbot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class NbotTelegramObserver(
    private val securityProperties: SecurityProperties,
    private val chatOrchestrator: ChatOrchestrator
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
            sendReply(message.chatId, "Document received, torrent functionality coming soon!")
        }
    }

    private fun sendReply(chatId: Long, text: String) {
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
