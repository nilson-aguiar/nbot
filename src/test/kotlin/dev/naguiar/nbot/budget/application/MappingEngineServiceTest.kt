package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.infrastructure.db.PayeeMappingRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MappingEngineServiceTest {

    private val repository = mockk<PayeeMappingRepository>()
    private val service = MappingEngineService(repository)

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
        service.map(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("amazon-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("Amazon.com")
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
        service.map(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("rewe-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("REWE Supermarket")
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
        service.map(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isEqualTo("amazon-id")
        assertThat(draft.suggestedPayeeName).isEqualTo("Amazon.com")
    }

    @Test
    fun `should not map if no pattern matches`() {
        // Given
        val mapping = PayeeMapping(
            bankPattern = "Amazon",
            actualPayeeName = "Amazon.com",
            actualPayeeId = "amazon-id"
        )
        every { repository.findAll() } returns listOf(mapping)

        val draft = createDraft(bankPayeeName = "Netflix", bankDescription = "Subscription")

        // When
        service.map(draft)

        // Then
        assertThat(draft.suggestedPayeeId).isNull()
        assertThat(draft.suggestedPayeeName).isNull()
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
