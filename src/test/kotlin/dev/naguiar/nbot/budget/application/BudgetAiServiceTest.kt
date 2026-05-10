package dev.naguiar.nbot.budget.application
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.ObjectProvider

class BudgetAiServiceTest {

    private val chatModel: ChatModel = mockk()
    private val chatModelProvider: ObjectProvider<ChatModel> = mockk()
    private val properties = ActualBudgetProperties()
    private val chatClient: ChatClient = mockk()
    private val chatClientBuilder: ChatClient.Builder = mockk()
    private val promptSpec: ChatClient.ChatClientRequestSpec = mockk()
    private val callResponseSpec: ChatClient.CallResponseSpec = mockk()

    @BeforeEach
    fun setUp() {
        mockkStatic(ChatClient::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ChatClient::class)
    }

    @Test
    fun `should suggest mapping correctly`() {
        // Given
        val expectedResponse = BudgetAiService.AiMappingResponse(
            payeeId = "payee-123",
            payeeName = "Netflix",
            isTransfer = false,
            targetAccountId = null,
            confidence = 0.95f,
            reasoning = "Matches Netflix exactly"
        )

        every { chatModelProvider.getIfAvailable() } returns chatModel
        every { chatModelProvider.ifAvailable } returns chatModel
        every { ChatClient.builder(chatModel) } returns chatClientBuilder
        every { chatClientBuilder.defaultSystem(any<String>()) } returns chatClientBuilder
        every { chatClientBuilder.build() } returns chatClient

        val service = BudgetAiService(chatModelProvider, properties)

        every { chatClient.prompt() } returns promptSpec
        every { promptSpec.user(any<String>()) } returns promptSpec
        every { promptSpec.call() } returns callResponseSpec
        every { callResponseSpec.entity(BudgetAiService.AiMappingResponse::class.java) } returns expectedResponse

        // When
        val result = service.suggestMapping(
            bankPayeeName = "NETFLIX.COM",
            bankDescription = "Monthly Subscription",
            knownPayees = listOf(BudgetAiService.ActualPayee("payee-123", "Netflix")),
            internalAccounts = listOf(BudgetAiService.InternalAccount("acc-1", "Main"))
        )

        // Then
        assertNotNull(result)
        assertEquals("payee-123", result?.payeeId)
        assertEquals("Netflix", result?.payeeName)
        assertEquals(false, result?.isTransfer)
        assertEquals(0.95f, result?.confidence)
    }
}
