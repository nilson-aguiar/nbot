package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.api.generated.AccountsApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.PayeesApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.TransactionsApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.Account
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.BudgetsBudgetSyncIdAccountsAccountIdTransactionsBatchPostRequest
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.Payee
import dev.naguiar.nbot.budget.infrastructure.api.generated.model.Transaction
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActualBudgetService(
    private val accountsApi: AccountsApi,
    private val payeesApi: PayeesApi,
    private val transactionsApi: TransactionsApi,
    private val properties: ActualBudgetProperties,
    private val transactionDraftRepository: TransactionDraftRepository,
) {
    private val log = LoggerFactory.getLogger(ActualBudgetService::class.java)
    private val encryptionPassword = properties.encryptionPassword.takeIf { it.isNotBlank() }

    fun getAccounts(): List<Account> {
        log.info("Fetching accounts from Actual Budget")
        return try {
            val response = accountsApi.budgetsBudgetSyncIdAccountsGet(properties.syncId, encryptionPassword)
            response.body?.data ?: emptyList()
        } catch (e: Exception) {
            log.error("Failed to fetch accounts from Actual Budget", e)
            emptyList()
        }
    }

    fun getPayees(): List<Payee> {
        log.info("Fetching payees from Actual Budget")
        return try {
            val response = payeesApi.budgetsBudgetSyncIdPayeesGet(properties.syncId, encryptionPassword)
            response.body?.data ?: emptyList()
        } catch (e: Exception) {
            log.error("Failed to fetch payees from Actual Budget", e)
            emptyList()
        }
    }

    fun syncApprovedDrafts(accountId: String) {
        if (accountId.isEmpty()) {
            log.error("Cannot sync approved drafts: default account ID is not configured")
            return
        }
        log.info("Syncing approved drafts to Actual Budget account: {}", accountId)
        val approvedDrafts = transactionDraftRepository.findByStatus(TransactionStatus.APPROVED)

        if (approvedDrafts.isEmpty()) {
            log.info("No approved drafts to sync")
            return
        }

        val actualTransactions =
            approvedDrafts.map { draft ->
                Transaction(
                    account = accountId,
                    date = draft.bookingDate.toString(),
                    amount = draft.amount.toInt(),
                    payee = draft.suggestedPayeeId,
                    payeeName = if (draft.suggestedPayeeId == null) draft.suggestedPayeeName else null,
                    notes = draft.bankDescription,
                    cleared = "true",
                )
            }

        try {
            val request =
                BudgetsBudgetSyncIdAccountsAccountIdTransactionsBatchPostRequest(
                    transactions = actualTransactions,
                    learnCategories = true,
                )
            transactionsApi.budgetsBudgetSyncIdAccountsAccountIdTransactionsBatchPost(
                properties.syncId,
                accountId,
                request,
                encryptionPassword,
            )

            val syncedDrafts =
                approvedDrafts.map { draft ->
                    draft.copy(status = TransactionStatus.SYNCED)
                }
            transactionDraftRepository.saveAll(syncedDrafts)
            log.info("Successfully synced {} drafts to Actual Budget", syncedDrafts.size)
        } catch (e: Exception) {
            log.error("Failed to sync drafts to Actual Budget", e)
        }
    }
}
