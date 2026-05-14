package dev.naguiar.nbot.budget.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.actual-budget")
data class ActualBudgetProperties(
    val url: String = "http://localhost:5007/v1",
    val apiKey: String = "",
    val syncId: String = "",
    val encryptionPassword: String = "",
    val defaultAccountId: String = "",
    val internalAccounts: List<String> =
        listOf(
            "Main account",
            "Joint account",
            "Month balance",
            "Extras",
            "Apolo",
            "Nilson",
            "Férias",
            "Poupança",
            "Mês seguinte",
            "Renata",
        ),
)
