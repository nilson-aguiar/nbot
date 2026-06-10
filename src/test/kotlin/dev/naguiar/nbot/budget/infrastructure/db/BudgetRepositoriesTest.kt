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
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(PayeeMappingRepositoryAdapter::class, TransactionDraftRepositoryAdapter::class)
class BudgetRepositoriesTest {
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }

    @Autowired
    lateinit var payeeMappingRepository: PayeeMappingRepositoryAdapter

    @Autowired
    lateinit var transactionDraftRepository: TransactionDraftRepositoryAdapter

    @Test
    fun `should save and retrieve PayeeMapping`() {
        val mapping =
            PayeeMapping(
                bankPattern = "AMZN",
                actualPayeeName = "Amazon",
                actualPayeeId = "uuid-123",
            )
        val saved = payeeMappingRepository.save(mapping)

        val retrieved = payeeMappingRepository.findById(saved.id)
        assertThat(retrieved?.actualPayeeName).isEqualTo("Amazon")
    }

    @Test
    fun `should delete PayeeMapping`() {
        val mapping =
            PayeeMapping(
                bankPattern = "DELETE_ME",
                actualPayeeName = "Delete Me",
                actualPayeeId = "uuid-delete",
            )
        val saved = payeeMappingRepository.save(mapping)
        payeeMappingRepository.deleteById(saved.id)

        val retrieved = payeeMappingRepository.findById(saved.id)
        assertThat(retrieved).isNull()
    }

    @Test
    fun `should find all PayeeMappings`() {
        payeeMappingRepository.save(PayeeMapping(bankPattern = "P1", actualPayeeName = "A1", actualPayeeId = "ID1"))
        payeeMappingRepository.save(PayeeMapping(bankPattern = "P2", actualPayeeName = "A2", actualPayeeId = "ID2"))

        val all = payeeMappingRepository.findAll()
        assertThat(all).hasSizeGreaterThanOrEqualTo(2)
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
        val saved = transactionDraftRepository.save(draft)

        val retrieved = transactionDraftRepository.findById(saved.id)
        assertThat(retrieved?.status).isEqualTo(TransactionStatus.PENDING)
    }

    @Test
    fun `should find TransactionDrafts by status`() {
        val draft =
            TransactionDraft(
                bookingDate = LocalDate.now(),
                amount = 500,
                currency = "EUR",
                bankPayeeName = "Spotify",
                bankDescription = "Music",
                exportFileId = "file-2",
                status = TransactionStatus.SYNCED,
            )
        transactionDraftRepository.save(draft)

        val processed = transactionDraftRepository.findByStatus(TransactionStatus.SYNCED)
        assertThat(processed).anyMatch { it.bankPayeeName == "Spotify" }
    }

    @Test
    fun `should save all TransactionDrafts`() {
        val drafts =
            listOf(
                TransactionDraft(
                    bookingDate = LocalDate.now(),
                    amount = 10,
                    currency = "EUR",
                    bankPayeeName = "B1",
                    bankDescription = "D1",
                    exportFileId = "F1",
                ),
                TransactionDraft(
                    bookingDate = LocalDate.now(),
                    amount = 20,
                    currency = "EUR",
                    bankPayeeName = "B2",
                    bankDescription = "D2",
                    exportFileId = "F1",
                ),
            )

        val saved = transactionDraftRepository.saveAll(drafts)
        assertThat(saved).hasSize(2)
    }

    @Test
    fun `should delete TransactionDraft`() {
        val draft =
            TransactionDraft(
                bookingDate = LocalDate.now(),
                amount = 100,
                currency = "EUR",
                bankPayeeName = "Target",
                bankDescription = "Shop",
                exportFileId = "F2",
            )
        val saved = transactionDraftRepository.save(draft)
        transactionDraftRepository.deleteById(saved.id)

        val retrieved = transactionDraftRepository.findById(saved.id)
        assertThat(retrieved).isNull()
    }
}
