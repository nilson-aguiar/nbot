package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaTransactionDraftRepository : JpaRepository<TransactionDraftEntity, UUID> {
    fun findByStatus(status: TransactionStatus): List<TransactionDraftEntity>
}
