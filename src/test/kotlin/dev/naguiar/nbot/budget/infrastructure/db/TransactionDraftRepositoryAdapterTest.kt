package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class TransactionDraftRepositoryAdapterTest {
    private val jpaRepository = mockk<JpaTransactionDraftRepository>()
    private val adapter = TransactionDraftRepositoryAdapter(jpaRepository)

    private fun createDraft() =
        TransactionDraft(
            bookingDate = LocalDate.now(),
            amount = 100,
            currency = "EUR",
            bankPayeeName = "bank",
            bankDescription = "desc",
            exportFileId = "file",
        )

    @Test
    fun `findByStatus should map entities to domain`() {
        val draft = createDraft()
        val entity = TransactionDraftEntity.fromDomain(draft)
        every { jpaRepository.findByStatus(TransactionStatus.PENDING) } returns listOf(entity)

        val result = adapter.findByStatus(TransactionStatus.PENDING)

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(draft.id)
        verify { jpaRepository.findByStatus(TransactionStatus.PENDING) }
    }

    @Test
    fun `findById should return domain when entity exists`() {
        val draft = createDraft()
        val entity = TransactionDraftEntity.fromDomain(draft)
        every { jpaRepository.findById(draft.id) } returns Optional.of(entity)

        val result = adapter.findById(draft.id)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(draft.id)
        verify { jpaRepository.findById(draft.id) }
    }

    @Test
    fun `findById should return null when entity does not exist`() {
        val id = UUID.randomUUID()
        every { jpaRepository.findById(id) } returns Optional.empty()

        val result = adapter.findById(id)

        assertThat(result).isNull()
        verify { jpaRepository.findById(id) }
    }

    @Test
    fun `save should map domain to entity and back`() {
        val domain = createDraft()
        val entity = TransactionDraftEntity.fromDomain(domain)
        every { jpaRepository.save(any()) } returns entity

        val result = adapter.save(domain)

        assertThat(result.id).isEqualTo(domain.id)
        verify { jpaRepository.save(match { it.id == domain.id }) }
    }

    @Test
    fun `saveAll should map domain list to entities and back`() {
        val domains = listOf(createDraft(), createDraft())
        val entities = domains.map { TransactionDraftEntity.fromDomain(it) }
        every { jpaRepository.saveAll(any<List<TransactionDraftEntity>>()) } returns entities

        val result = adapter.saveAll(domains)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactlyInAnyOrderElementsOf(domains.map { it.id })
        verify { jpaRepository.saveAll(match<List<TransactionDraftEntity>> { it.size == 2 }) }
    }

    @Test
    fun `deleteById should delegate to jpa repository`() {
        val id = UUID.randomUUID()
        every { jpaRepository.deleteById(id) } returns Unit

        adapter.deleteById(id)

        verify { jpaRepository.deleteById(id) }
    }
}
