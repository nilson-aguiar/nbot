package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.api.*
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class ActualBudgetServiceTest {

    private val api = mockk<ActualBudgetApi>()
    private val properties = ActualBudgetProperties(url = "http://localhost:5006", apiKey = "secret-key", syncId = "sync-123")
    private val repository = mockk<TransactionDraftRepository>()
    private val service = ActualBudgetService(api, properties, repository)

    @Test
    fun `should fetch accounts successfully`() {
        val expectedAccounts = listOf(
            ActualAccount("1", "Main Account", "bank", offbudget = false, closed = false)
        )
        every { api.getAccounts("secret-key", "sync-123") } returns ActualAccountResponse(expectedAccounts)

        val result = service.getAccounts()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Main Account")
    }

    @Test
    fun `should fetch payees successfully`() {
        val expectedPayees = listOf(
            ActualPayee("p1", "Amazon")
        )
        every { api.getPayees("secret-key", "sync-123") } returns ActualPayeeResponse(expectedPayees)

        val result = service.getPayees()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Amazon")
    }

    @Test
    fun `should sync approved drafts`() {
        val draft = TransactionDraft(
            bookingDate = LocalDate.now(),
            amount = 1000L,
            currency = "EUR",
            bankPayeeName = "Amazon",
            bankDescription = "Cloud service",
            suggestedPayeeId = "payee-123",
            status = TransactionStatus.APPROVED,
            exportFileId = "file-1"
        )
        every { repository.findByStatus(TransactionStatus.APPROVED) } returns listOf(draft)
        every { api.addTransactions(any(), any(), any(), any()) } returns ResponseEntity.ok().build()
        every { repository.saveAll(any<List<TransactionDraft>>()) } returns listOf(draft)

        service.syncApprovedDrafts("account-456")

        verify { api.addTransactions("secret-key", "sync-123", "account-456", match { 
            it.transactions.size == 1 && it.transactions[0].accountId == "account-456" 
        }) }
        verify { repository.saveAll(match<List<TransactionDraft>> { it[0].status == TransactionStatus.SYNCED }) }
    }
}
