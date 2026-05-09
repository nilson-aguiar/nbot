package dev.naguiar.nbot.budget.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
class PayeeMapping(
    @Id
    val id: UUID = UUID.randomUUID(),
    var bankPattern: String,
    var actualPayeeName: String,
    var actualPayeeId: String,
    var isInternalTransfer: Boolean = false,
    var targetAccountId: String? = null,
    var confidenceScore: Float = 1.0f
)
