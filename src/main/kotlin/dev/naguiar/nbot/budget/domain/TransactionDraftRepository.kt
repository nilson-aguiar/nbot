package dev.naguiar.nbot.budget.domain

import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface TransactionDraftRepository {
    @Transactional(readOnly = true)
    fun findByStatus(status: TransactionStatus): List<TransactionDraft>

    @Transactional(readOnly = true)
    fun findById(id: UUID): TransactionDraft?

    @Transactional
    fun save(draft: TransactionDraft): TransactionDraft

    @Transactional
    fun saveAll(drafts: List<TransactionDraft>): List<TransactionDraft>

    @Transactional
    fun deleteById(id: UUID)
}
