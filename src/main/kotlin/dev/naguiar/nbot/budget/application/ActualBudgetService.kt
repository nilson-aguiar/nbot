package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.api.*
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActualBudgetService(
    private val actualBudgetApi: ActualBudgetApi,
    private val properties: ActualBudgetProperties,
    private val transactionDraftRepository: TransactionDraftRepository
) {
    private val log = LoggerFactory.getLogger(ActualBudgetService::class.java)

    fun getAccounts(): List<ActualAccount> {
        log.info("Fetching accounts from Actual Budget")
        return try {
            val response = actualBudgetApi.getAccounts(properties.apiKey, properties.syncId)
            response.data
        } catch (e: Exception) {
            log.error("Failed to fetch accounts from Actual Budget", e)
            emptyList()
        }
    }

    fun getPayees(): List<ActualPayee> {
        log.info("Fetching payees from Actual Budget")
        return try {
            val response = actualBudgetApi.getPayees(properties.apiKey, properties.syncId)
            response.data
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

        val actualTransactions = approvedDrafts.map { draft ->
            ActualTransaction(
                accountId = accountId,
                date = draft.bookingDate.toString(),
                amount = draft.amount,
                payeeId = draft.suggestedPayeeId,
                payeeName = draft.suggestedPayeeName,
                notes = draft.bankDescription
            )
        }

        try {
            val request = ActualTransactionRequest(actualTransactions)
            actualBudgetApi.addTransactions(properties.apiKey, properties.syncId, accountId, request)
            
            approvedDrafts.forEach { draft ->
                draft.status = TransactionStatus.SYNCED
            }
            transactionDraftRepository.saveAll(approvedDrafts)
            log.info("Successfully synced {} drafts to Actual Budget", approvedDrafts.size)
        } catch (e: Exception) {
            log.error("Failed to sync drafts to Actual Budget", e)
        }
    }
}
