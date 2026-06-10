package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.api.generated.AccountsApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.PayeesApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.TransactionsApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.Account
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.BudgetsBudgetSyncIdAccountsGet200Response
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.BudgetsBudgetSyncIdPayeesGet200Response
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.Payee
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class ActualBudgetServiceTest {
    private val accountsApi = mockk<AccountsApi>()
    private val payeesApi = mockk<PayeesApi>()
    private val transactionsApi = mockk<TransactionsApi>()
    private val properties =
        ActualBudgetProperties(url = "http://localhost:5006", apiKey = "secret-key", syncId = "sync-123")
    private val repository = mockk<TransactionDraftRepository>()
    private val service = ActualBudgetService(accountsApi, payeesApi, transactionsApi, properties, repository)

    @Test
    fun `should fetch accounts successfully`() {
        val expectedAccounts =
            listOf(
                Account(closed = false, id = "1", name = "Main Account", offbudget = false),
            )
        every { accountsApi.budgetsBudgetSyncIdAccountsGet("sync-123", null) } returns
            ResponseEntity.ok(
                BudgetsBudgetSyncIdAccountsGet200Response(expectedAccounts),
            )

        val result = service.getAccounts()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Main Account")
    }

    @Test
    fun `should fetch payees successfully`() {
        val expectedPayees =
            listOf(
                Payee(name = "Amazon", id = "p1"),
            )
        every { payeesApi.budgetsBudgetSyncIdPayeesGet("sync-123", null) } returns
            ResponseEntity.ok(
                BudgetsBudgetSyncIdPayeesGet200Response(expectedPayees),
            )

        val result = service.getPayees()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Amazon")
    }

    @Test
    fun `should sync approved drafts`() {
        val draft =
            TransactionDraft(
                bookingDate = LocalDate.now(),
                amount = 1000L,
                currency = "EUR",
                bankPayeeName = "Amazon",
                bankDescription = "Cloud service",
                suggestedPayeeId = "payee-123",
                status = TransactionStatus.APPROVED,
                exportFileId = "file-1",
            )
        every { repository.findByStatus(TransactionStatus.APPROVED) } returns listOf(draft)
        every {
            transactionsApi.budgetsBudgetSyncIdAccountsAccountIdTransactionsBatchPost(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            ResponseEntity.ok().build()
        every { repository.saveAll(any<List<TransactionDraft>>()) } returns listOf(draft)

        service.syncApprovedDrafts("account-456")

        verify {
            transactionsApi.budgetsBudgetSyncIdAccountsAccountIdTransactionsBatchPost(
                "sync-123",
                "account-456",
                match {
                    it.transactions.size == 1 && it.transactions[0].account == "account-456"
                },
                null,
            )
        }
        verify { repository.saveAll(match<List<TransactionDraft>> { it[0].status == TransactionStatus.SYNCED }) }
    }
}
