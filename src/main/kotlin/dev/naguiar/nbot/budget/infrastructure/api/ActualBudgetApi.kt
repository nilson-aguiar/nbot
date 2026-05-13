package dev.naguiar.nbot.budget.infrastructure.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange

interface ActualBudgetApi {
    @GetExchange("/budgets/{budgetSyncId}/accounts")
    fun getAccounts(
        @RequestHeader("x-api-key") apiKey: String,
        @PathVariable("budgetSyncId") budgetSyncId: String
    ): ActualAccountResponse

    @GetExchange("/budgets/{budgetSyncId}/payees")
    fun getPayees(
        @RequestHeader("x-api-key") apiKey: String,
        @PathVariable("budgetSyncId") budgetSyncId: String
    ): ActualPayeeResponse

    @PostExchange("/budgets/{budgetSyncId}/accounts/{accountId}/transactions")
    fun addTransactions(
        @RequestHeader("x-api-key") apiKey: String,
        @PathVariable("budgetSyncId") budgetSyncId: String,
        @PathVariable("accountId") accountId: String,
        @RequestBody body: ActualTransactionRequest
    ): ResponseEntity<Void>
}
