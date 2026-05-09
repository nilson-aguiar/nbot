package dev.naguiar.nbot.budget.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.LocalDate
import java.util.UUID

@Entity
class TransactionDraft(
    @Id
    val id: UUID = UUID.randomUUID(),
    var bookingDate: LocalDate,
    var amount: Long,
    var currency: String,
    var bankPayeeName: String,
    var bankDescription: String,
    var suggestedPayeeId: String? = null,
    var suggestedPayeeName: String? = null,
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus = TransactionStatus.PENDING,
    var exportFileId: String
)
