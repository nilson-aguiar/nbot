package dev.naguiar.nbot.budget.domain

import java.time.LocalDate
import java.util.UUID

data class TransactionDraft(
    val id: UUID = UUID.randomUUID(),
    val bookingDate: LocalDate,
    val amount: Long,
    val currency: String,
    val bankPayeeName: String,
    val bankDescription: String,
    val suggestedPayeeId: String? = null,
    val suggestedPayeeName: String? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val exportFileId: String,
)
