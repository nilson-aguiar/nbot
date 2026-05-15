package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.PayeeMapping
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class PayeeMappingRepositoryAdapterTest {
    private val jpaRepository = mockk<JpaPayeeMappingRepository>()
    private val adapter = PayeeMappingRepositoryAdapter(jpaRepository)

    @Test
    fun `findAll should map entities to domain`() {
        val entity =
            PayeeMappingEntity(
                id = UUID.randomUUID(),
                bankPattern = "pattern",
                actualPayeeName = "payee",
                actualPayeeId = "payee-id",
                isInternalTransfer = true,
                targetAccountId = "target-id",
                confidenceScore = 0.9f,
            )
        every { jpaRepository.findAll() } returns listOf(entity)

        val result = adapter.findAll()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(entity.id)
        assertThat(result[0].bankPattern).isEqualTo("pattern")
        assertThat(result[0].actualPayeeName).isEqualTo("payee")
        assertThat(result[0].actualPayeeId).isEqualTo("payee-id")
        assertThat(result[0].isInternalTransfer).isTrue()
        assertThat(result[0].targetAccountId).isEqualTo("target-id")
        assertThat(result[0].confidenceScore).isEqualTo(0.9f)
        verify { jpaRepository.findAll() }
    }

    @Test
    fun `findById should return domain when entity exists`() {
        val id = UUID.randomUUID()
        val entity =
            PayeeMappingEntity(
                id = id,
                bankPattern = "pattern",
                actualPayeeName = "payee",
                actualPayeeId = "payee-id",
                isInternalTransfer = true,
                targetAccountId = "target-id",
                confidenceScore = 0.9f,
            )
        every { jpaRepository.findById(id) } returns Optional.of(entity)

        val result = adapter.findById(id)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(id)
        assertThat(result?.bankPattern).isEqualTo("pattern")
        assertThat(result?.actualPayeeName).isEqualTo("payee")
        assertThat(result?.actualPayeeId).isEqualTo("payee-id")
        assertThat(result?.isInternalTransfer).isTrue()
        assertThat(result?.targetAccountId).isEqualTo("target-id")
        assertThat(result?.confidenceScore).isEqualTo(0.9f)
    }

    @Test
    fun `findById should return null when entity does not exist`() {
        val id = UUID.randomUUID()
        every { jpaRepository.findById(id) } returns Optional.empty()

        val result = adapter.findById(id)

        assertThat(result).isNull()
    }

    @Test
    fun `save should map domain to entity and back`() {
        val domain =
            PayeeMapping(
                bankPattern = "pattern",
                actualPayeeName = "payee",
                actualPayeeId = "payee-id",
                isInternalTransfer = true,
                targetAccountId = "target-id",
                confidenceScore = 0.9f,
            )
        val entity = PayeeMappingEntity.fromDomain(domain)
        every { jpaRepository.save(any()) } returns entity

        val result = adapter.save(domain)

        assertThat(result.id).isEqualTo(domain.id)
        assertThat(result.bankPattern).isEqualTo("pattern")
        assertThat(result.actualPayeeName).isEqualTo("payee")
        assertThat(result.actualPayeeId).isEqualTo("payee-id")
        assertThat(result.isInternalTransfer).isTrue()
        assertThat(result.targetAccountId).isEqualTo("target-id")
        assertThat(result.confidenceScore).isEqualTo(0.9f)
        verify {
            jpaRepository.save(
                match {
                    it.id == domain.id &&
                        it.bankPattern == "pattern" &&
                        it.actualPayeeName == "payee" &&
                        it.actualPayeeId == "payee-id" &&
                        it.isInternalTransfer == true &&
                        it.targetAccountId == "target-id" &&
                        it.confidenceScore == 0.9f
                },
            )
        }
    }

    @Test
    fun `deleteById should delegate to jpa repository`() {
        val id = UUID.randomUUID()
        every { jpaRepository.deleteById(id) } returns Unit

        adapter.deleteById(id)

        verify { jpaRepository.deleteById(id) }
    }
}
