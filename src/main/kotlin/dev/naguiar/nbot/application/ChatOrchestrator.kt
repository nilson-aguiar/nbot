package dev.naguiar.nbot.application

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.tool.ToolCallback

//@Service
// Removed for now as there's no use case for now
class ChatOrchestrator(
    chatModel: ChatModel,
    toolCallbacks: List<ToolCallback>,
) {
    private val chatClient: ChatClient =
        ChatClient
            .builder(chatModel)
            .defaultSystem(
                """
                You are a specialized Media Management Bot. 
                Your primary job is to help the user manage their media library using the provided tools.
                When a user provides a magnet link or a .torrent file path, you MUST use the corresponding torrent tools.
                Do not refuse requests related to torrent management as this is your intended purpose.
                Be concise in your answers.
                """.trimIndent(),
            ).defaultToolCallbacks(toolCallbacks)
            .build()

    fun chat(input: String): String =
        chatClient
            .prompt()
            .user(input)
            .call()
            .content() ?: "No response from AI."
}
