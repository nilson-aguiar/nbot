package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class TransactionDraftRepositoryAdapter(
    private val jpaRepository: JpaTransactionDraftRepository,
) : TransactionDraftRepository {
    override fun findByStatus(status: TransactionStatus): List<TransactionDraft> = jpaRepository.findByStatus(status).map { it.toDomain() }

    override fun findById(id: UUID): TransactionDraft? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun save(draft: TransactionDraft): TransactionDraft = jpaRepository.save(draft.toEntity()).toDomain()

    override fun saveAll(drafts: List<TransactionDraft>): List<TransactionDraft> =
        jpaRepository
            .saveAll(
                drafts.map {
                    it.toEntity()
                },
            ).map { it.toDomain() }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}
