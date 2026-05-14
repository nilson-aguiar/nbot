package dev.naguiar.nbot.budget.domain

import java.util.UUID


data class PayeeMapping(
    val id: UUID = UUID.randomUUID(),
    val bankPattern: String,
    val actualPayeeName: String,
    val actualPayeeId: String,
    val isInternalTransfer: Boolean = false,
    val targetAccountId: String? = null,
    val confidenceScore: Float = 1.0f,
)
