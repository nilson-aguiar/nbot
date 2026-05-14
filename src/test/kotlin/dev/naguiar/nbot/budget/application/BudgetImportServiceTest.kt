package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BudgetImportServiceTest {
    private val camtParserService = mockk<CamtParserService>()
    private val mappingEngineService = mockk<MappingEngineService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>(relaxed = true)
    private val actualBudgetService = mockk<ActualBudgetService>(relaxed = true)
    private val budgetImportService =
        BudgetImportService(
            camtParserService,
            mappingEngineService,
            transactionDraftRepository,
            actualBudgetService,
        )

    @Test
    fun `should orchestrate CAMT import successfully`() {
        // Given
        val inputStream = mockk<InputStream>()
        val draft1 = createDraft("Amazon")
        val draft2 = createDraft("Netflix")
        val drafts = listOf(draft1, draft2)

        every { camtParserService.parse(any<InputStream>(), any()) } returns drafts
        every { transactionDraftRepository.saveAll(drafts) } returns drafts

        // When
        val exportFileId = budgetImportService.importCamt(inputStream)

        // Then
        assertThat(exportFileId).isNotNull()
        verify { camtParserService.parse(any<InputStream>(), exportFileId) }
        verify { transactionDraftRepository.saveAll(drafts) }
    }

    private fun createDraft(bankPayeeName: String) =
        TransactionDraft(
            bookingDate = LocalDate.now(),
            amount = 1000,
            currency = "EUR",
            bankPayeeName = bankPayeeName,
            bankDescription = "Description",
            exportFileId = "temp-id",
        )

    @Test
    fun `reEvaluateAsync processes sequentially and manages lock`() {
        val draftId = UUID.randomUUID()
        val latch = CountDownLatch(1)
        var processedCount = 0

        // Mocking behavior
        every { actualBudgetService.getPayees() } returns emptyList()
        val pendingDraft =
            createDraft(
                "Netflix",
            ).copy(status = dev.naguiar.nbot.budget.domain.TransactionStatus.PENDING)
        every { transactionDraftRepository.findById(draftId) } returns pendingDraft
        every { mappingEngineService.applyMappings(any(), any()) } returns pendingDraft
        every { transactionDraftRepository.save(any()) } returns pendingDraft

        budgetImportService.reEvaluateAsync(
            draftIds = listOf(draftId),
            onProgress = { processedCount++ },
            onComplete = { latch.countDown() },
        )

        latch.await(5, TimeUnit.SECONDS)
        assertThat(budgetImportService.isEvaluating.get()).isFalse()
        assertThat(processedCount).isEqualTo(1)
    }
}
