package dev.naguiar.nbot

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

@Service
class ChatOrchestrator(chatModel: ChatModel) {
    private val chatClient: ChatClient = ChatClient.builder(chatModel)
        .defaultSystem("You are a helpful Telegram bot assistant with skills. Be concise in your answers.")
        .build()

    fun chat(input: String): String {
        return chatClient.prompt()
            .functions("addTorrentMagnet")
            .user(input)
            .call()
            .content() ?: "No response from AI."
    }
}
