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

data class ActualTransaction(
    val accountId: String,
    val date: String,
    val amount: Long,
    val payeeId: String?,
    val payeeName: String?,
    val notes: String?,
    val cleared: Boolean = true
)

data class ActualTransactionRequest(
    val transactions: List<ActualTransaction>
)

data class ActualPayeeResponse(
    val data: List<ActualPayee>
)

data class ActualPayee(
    val id: String,
    val name: String
)
