package dev.naguiar.nbot.budget.infrastructure.api

import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange

interface ActualBudgetApi {
    @GetExchange("/accounts")
    fun getAccounts(
        @RequestHeader("x-api-key") apiKey: String
    ): ActualAccountResponse
}
