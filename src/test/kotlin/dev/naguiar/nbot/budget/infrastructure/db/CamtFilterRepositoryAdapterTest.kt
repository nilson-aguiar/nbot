package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.CamtFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CamtFilterRepositoryAdapterTest {
    private val jpaRepository = mockk<JpaCamtFilterRepository>()
    private val adapter = CamtFilterRepositoryAdapter(jpaRepository)

    @Test
    fun `findAll should map entities to domain`() {
        val entity =
            CamtFilterEntity(
                id = UUID.randomUUID(),
                namePattern = "test-name",
                ibanPattern = "test-iban",
                isStrict = true,
            )
        every { jpaRepository.findAll() } returns listOf(entity)

        val result = adapter.findAll()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(entity.id)
        assertThat(result[0].namePattern).isEqualTo("test-name")
        assertThat(result[0].ibanPattern).isEqualTo("test-iban")
        assertThat(result[0].isStrict).isTrue()
        verify { jpaRepository.findAll() }
    }

    @Test
    fun `save should map domain to entity and back`() {
        val domain =
            CamtFilter(
                namePattern = "name",
                ibanPattern = "iban",
                isStrict = false,
            )
        val entity = CamtFilterEntity.fromDomain(domain)
        every { jpaRepository.save(any()) } returns entity

        val result = adapter.save(domain)

        assertThat(result.id).isEqualTo(domain.id)
        assertThat(result.namePattern).isEqualTo("name")
        assertThat(result.ibanPattern).isEqualTo("iban")
        assertThat(result.isStrict).isFalse()
        verify {
            jpaRepository.save(
                match {
                    it.id == domain.id &&
                        it.namePattern == "name" &&
                        it.ibanPattern == "iban" &&
                        it.isStrict == false
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
