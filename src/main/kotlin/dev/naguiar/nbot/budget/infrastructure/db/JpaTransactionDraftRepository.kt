package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionStatus
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTransactionDraftRepository : JpaRepository<TransactionDraftEntity, UUID> {
    fun findByStatus(status: TransactionStatus): List<TransactionDraftEntity>
}
