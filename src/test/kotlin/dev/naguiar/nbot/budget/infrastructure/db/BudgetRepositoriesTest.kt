package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BudgetRepositoriesTest {
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }

    @Autowired
    lateinit var payeeMappingRepository: JpaPayeeMappingRepository

    @Autowired
    lateinit var transactionDraftRepository: JpaTransactionDraftRepository

    @Test
    fun `should save and retrieve PayeeMapping`() {
        val mapping =
            PayeeMapping(
                bankPattern = "AMZN",
                actualPayeeName = "Amazon",
                actualPayeeId = "uuid-123",
            )
        val saved = payeeMappingRepository.save(PayeeMappingEntity.fromDomain(mapping)).toDomain()

        val retrieved = payeeMappingRepository.findById(saved.id).orElseThrow().toDomain()
        assertThat(retrieved.actualPayeeName).isEqualTo("Amazon")
    }

    @Test
    fun `should save and retrieve TransactionDraft`() {
        val draft =
            TransactionDraft(
                bookingDate = LocalDate.now(),
                amount = 1250,
                currency = "EUR",
                bankPayeeName = "Netflix",
                bankDescription = "Monthly sub",
                exportFileId = "file-1",
            )
        val saved = transactionDraftRepository.save(TransactionDraftEntity.fromDomain(draft)).toDomain()

        val retrieved = transactionDraftRepository.findById(saved.id).orElseThrow().toDomain()
        assertThat(retrieved.status).isEqualTo(TransactionStatus.PENDING)
    }
}
