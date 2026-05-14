package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.PayeeMapping
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "payee_mapping")
class PayeeMappingEntity(
    @Id
    var id: UUID,
    var bankPattern: String,
    var actualPayeeName: String,
    var actualPayeeId: String,
    var isInternalTransfer: Boolean = false,
    var targetAccountId: String? = null,
    var confidenceScore: Float = 1.0f,
) {
    fun toDomain(): PayeeMapping =
        PayeeMapping(
            id = id,
            bankPattern = bankPattern,
            actualPayeeName = actualPayeeName,
            actualPayeeId = actualPayeeId,
            isInternalTransfer = isInternalTransfer,
            targetAccountId = targetAccountId,
            confidenceScore = confidenceScore,
        )

    companion object {
        fun fromDomain(domain: PayeeMapping): PayeeMappingEntity =
            PayeeMappingEntity(
                id = domain.id,
                bankPattern = domain.bankPattern,
                actualPayeeName = domain.actualPayeeName,
                actualPayeeId = domain.actualPayeeId,
                isInternalTransfer = domain.isInternalTransfer,
                targetAccountId = domain.targetAccountId,
                confidenceScore = domain.confidenceScore,
            )
    }
}

fun PayeeMapping.toEntity(): PayeeMappingEntity = PayeeMappingEntity.fromDomain(this)
