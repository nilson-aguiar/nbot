package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import dev.naguiar.nbot.budget.infrastructure.db.PayeeMappingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MappingEngineServiceTest {

    private val repository = mockk<PayeeMappingRepository>()
    private val budgetAiService = mockk<BudgetAiService>()
    private val properties = ActualBudgetProperties(internalAccounts = listOf("Savings"))
    private val service = MappingEngineService(repository, budgetAiService, properties)

    @Test
    fun `should map transaction by bankPayeeName`() {
        // Given
        val mapping = PayeeMapping(
            bankPattern = "Amazon",
            actualPayeeName = "Amazon.com",
            actualPayeeId = "amazon-id"
        )
        every { repository.findAll() } returns listOf(mapping)

        val draft = createDraft(bankPayeeName = "AMAZON MKTPLACE", bankDescription = "something else")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("amazon-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("Amazon.com")
        verify(exactly = 0) { budgetAiService.suggestMapping(any(), any(), any(), any()) }
    }

    @Test
    fun `should map transaction by bankDescription`() {
        // Given
        val mapping = PayeeMapping(
            bankPattern = "REWE",
            actualPayeeName = "REWE Supermarket",
            actualPayeeId = "rewe-id"
        )
        every { repository.findAll() } returns listOf(mapping)

        val draft = createDraft(bankPayeeName = "Unknown", bankDescription = "PURCHASE AT REWE STORE")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("rewe-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("REWE Supermarket")
        verify(exactly = 0) { budgetAiService.suggestMapping(any(), any(), any(), any()) }
    }

    @Test
    fun `should use first matching pattern`() {
        // Given
        val mapping1 = PayeeMapping(
            bankPattern = "Amazon",
            actualPayeeName = "Amazon.com",
            actualPayeeId = "amazon-id"
        )
        val mapping2 = PayeeMapping(
            bankPattern = "MKTPLACE",
            actualPayeeName = "Generic Marketplace",
            actualPayeeId = "generic-id"
        )
        every { repository.findAll() } returns listOf(mapping1, mapping2)

        val draft = createDraft(bankPayeeName = "AMAZON MKTPLACE")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("amazon-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("Amazon.com")
    }

    @Test
    fun `should use AI fallback if no pattern matches`() {
        // Given
        every { repository.findAll() } returns emptyList()
        val aiResponse = BudgetAiService.AiMappingResponse(
            payeeId = "ai-payee-id",
            payeeName = "AI Payee",
            isTransfer = false,
            targetAccountId = null,
            confidence = 0.8f,
            reasoning = "Looks like AI Payee"
        )
        every {
            budgetAiService.suggestMapping(
                "Netflix",
                "Subscription",
                emptyList(),
                listOf(BudgetAiService.InternalAccount("Savings", "Savings"))
            )
        } returns aiResponse

        val draft = createDraft(bankPayeeName = "Netflix", bankDescription = "Subscription")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("ai-payee-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("AI Payee")
    }

    @Test
    fun `should ignore AI fallback if confidence is too low`() {
        // Given
        every { repository.findAll() } returns emptyList()
        val aiResponse = BudgetAiService.AiMappingResponse(
            payeeId = "ai-payee-id",
            payeeName = "AI Payee",
            isTransfer = false,
            targetAccountId = null,
            confidence = 0.5f,
            reasoning = "Not sure"
        )
        every {
            budgetAiService.suggestMapping(any(), any(), any(), any())
        } returns aiResponse

        val draft = createDraft(bankPayeeName = "Netflix", bankDescription = "Subscription")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isNull()
        assertThat(draft.suggestedPayeeName).isNull()
    }

    @Test
    fun `should set status to IGNORED if AI identifies it as a transfer`() {
        // Given
        every { repository.findAll() } returns emptyList()
        val aiResponse = BudgetAiService.AiMappingResponse(
            payeeId = null,
            payeeName = null,
            isTransfer = true,
            targetAccountId = "savings-id",
            confidence = 0.9f,
            reasoning = "Internal transfer"
        )
        every {
            budgetAiService.suggestMapping(any(), any(), any(), any())
        } returns aiResponse

        val draft = createDraft(bankPayeeName = "Transfer to Savings")

        // When
        service.applyMappings(draft)

        // Then
        assertThat(draft.status).isEqualTo(TransactionStatus.IGNORED)
    }

    private fun createDraft(bankPayeeName: String = "", bankDescription: String = "") = TransactionDraft(
        bookingDate = LocalDate.now(),
        amount = 1000,
        currency = "EUR",
        bankPayeeName = bankPayeeName,
        bankDescription = bankDescription,
        exportFileId = "file-id"
    )
}
