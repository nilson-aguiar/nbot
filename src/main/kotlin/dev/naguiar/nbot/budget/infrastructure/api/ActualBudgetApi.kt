package dev.naguiar.nbot.budget.infrastructure.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange

interface ActualBudgetApi {
    @GetExchange("/accounts")
    fun getAccounts(
        @RequestHeader("x-api-key") apiKey: String
    ): ActualAccountResponse

    @GetExchange("/payees")
    fun getPayees(
        @RequestHeader("x-api-key") apiKey: String
    ): ActualPayeeResponse

    @PostExchange("/transactions")
    fun addTransactions(
        @RequestHeader("x-api-key") apiKey: String,
        @RequestBody body: ActualTransactionRequest
    ): ResponseEntity<Void>
}
