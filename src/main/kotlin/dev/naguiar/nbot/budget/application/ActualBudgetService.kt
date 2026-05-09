package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.infrastructure.api.ActualAccount
import dev.naguiar.nbot.budget.infrastructure.api.ActualBudgetApi
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActualBudgetService(
    private val actualBudgetApi: ActualBudgetApi,
    private val properties: ActualBudgetProperties
) {
    private val log = LoggerFactory.getLogger(ActualBudgetService::class.java)

    fun getAccounts(): List<ActualAccount> {
        log.info("Fetching accounts from Actual Budget")
        return try {
            val response = actualBudgetApi.getAccounts(properties.apiKey)
            response.data
        } catch (e: Exception) {
            log.error("Failed to fetch accounts from Actual Budget", e)
            emptyList()
        }
    }
}
