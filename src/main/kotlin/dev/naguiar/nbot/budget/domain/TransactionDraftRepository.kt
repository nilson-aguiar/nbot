package dev.naguiar.nbot.budget.domain

import java.util.UUID
import org.springframework.transaction.annotation.Transactional

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
