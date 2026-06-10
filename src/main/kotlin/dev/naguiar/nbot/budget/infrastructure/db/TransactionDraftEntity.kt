package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "transaction_draft")
class TransactionDraftEntity(
    @Id
    var id: UUID,
    var bookingDate: LocalDate,
    var amount: Long,
    var currency: String,
    var bankPayeeName: String,
    var bankDescription: String,
    var suggestedPayeeId: String? = null,
    var suggestedPayeeName: String? = null,
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus,
    var exportFileId: String,
) {
    fun toDomain(): TransactionDraft =
        TransactionDraft(
            id = id,
            bookingDate = bookingDate,
            amount = amount,
            currency = currency,
            bankPayeeName = bankPayeeName,
            bankDescription = bankDescription,
            suggestedPayeeId = suggestedPayeeId,
            suggestedPayeeName = suggestedPayeeName,
            status = status,
            exportFileId = exportFileId,
        )

    companion object {
        fun fromDomain(draft: TransactionDraft): TransactionDraftEntity =
            TransactionDraftEntity(
                id = draft.id,
                bookingDate = draft.bookingDate,
                amount = draft.amount,
                currency = draft.currency,
                bankPayeeName = draft.bankPayeeName,
                bankDescription = draft.bankDescription,
                suggestedPayeeId = draft.suggestedPayeeId,
                suggestedPayeeName = draft.suggestedPayeeName,
                status = draft.status,
                exportFileId = draft.exportFileId,
            )
    }
}

fun TransactionDraft.toEntity(): TransactionDraftEntity = TransactionDraftEntity.fromDomain(this)
