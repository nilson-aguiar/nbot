package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class BudgetAiService(
    chatModelProvider: ObjectProvider<ChatModel>,
    private val properties: ActualBudgetProperties
) {
    private val chatClient = chatModelProvider.ifAvailable?.let { chatModel ->
        ChatClient.builder(chatModel)
            .defaultSystem("""
                You are a financial assistant helping to map bank transactions to budget payees.
                Your task is to analyze a bank transaction and suggest the most likely payee from a provided list.
                If the transaction appears to be a transfer between internal accounts, identify it as such.
                
                Return your response in structured JSON format with the following fields:
                - payeeId: The ID of the suggested payee from the list, or null if no match found.
                - payeeName: The name of the suggested payee from the list, or null.
                - isTransfer: Boolean indicating if this is an internal transfer between the user's accounts.
                - targetAccountId: The ID of the target internal account if it's a transfer, or null.
                - confidence: A float between 0.0 and 1.0 representing your confidence in this mapping.
                - reasoning: A brief explanation of why you chose this mapping.
            """.trimIndent())
            .build()
    }

    fun suggestMapping(
        bankPayeeName: String,
        bankDescription: String,
        knownPayees: List<ActualPayee>,
        internalAccounts: List<InternalAccount>
    ): AiMappingResponse? {
        if (chatClient == null) return null

        val promptText = """
            Analyze the following bank transaction:
            Bank Payee Name: $bankPayeeName
            Bank Description: $bankDescription
            
            Known Payees:
            ${knownPayees.joinToString("\n") { "- ${it.name} (ID: ${it.id})" }}
            
            Internal Accounts:
            ${internalAccounts.joinToString("\n") { "- ${it.name} (ID: ${it.id})" }}
            
            Find the best match or identify if it's an internal transfer.
        """.trimIndent()

        return chatClient.prompt()
            .user(promptText)
            .call()
            .entity(AiMappingResponse::class.java)
    }

    data class ActualPayee(val id: String, val name: String)
    data class InternalAccount(val id: String, val name: String)
    
    data class AiMappingResponse(
        val payeeId: String?,
        val payeeName: String?,
        val isTransfer: Boolean,
        val targetAccountId: String?,
        val confidence: Float,
        val reasoning: String
    )
}
