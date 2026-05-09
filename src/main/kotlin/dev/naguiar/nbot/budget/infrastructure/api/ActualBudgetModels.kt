package dev.naguiar.nbot.budget.infrastructure.api

data class ActualAccountResponse(
    val data: List<ActualAccount>
)

data class ActualAccount(
    val id: String,
    val name: String,
    val type: String,
    val offbudget: Boolean,
    val closed: Boolean
)
