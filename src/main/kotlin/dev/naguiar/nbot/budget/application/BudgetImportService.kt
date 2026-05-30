package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

@Service
class BudgetImportService(
    private val camtParserService: CamtParserService,
    private val mappingEngineService: MappingEngineService,
    private val transactionDraftRepository: TransactionDraftRepository,
    private val actualBudgetService: ActualBudgetService,
) {
    private val logger = LoggerFactory.getLogger(BudgetImportService::class.java)

    private val _isEvaluating = AtomicBoolean(false)
    val isEvaluating: AtomicBoolean get() = _isEvaluating

    @Async
    fun reEvaluateAsync(
        draftIds: List<UUID>,
        onProgress: (TransactionDraft) -> Unit,
        onComplete: () -> Unit,
    ) {
        if (draftIds.isEmpty() || !_isEvaluating.compareAndSet(false, true)) {
            return
        }

        try {
            val payees = actualBudgetService.getPayees()
            for (id in draftIds) {
                transactionDraftRepository.findById(id)?.let { draft ->
                    if (draft.status == TransactionStatus.PENDING) {
                        val updatedDraft = mappingEngineService.applyMappings(draft, payees)
                        transactionDraftRepository.save(updatedDraft)
                        onProgress(updatedDraft)
                    }
                }
            }
        } finally {
            _isEvaluating.set(false)
            onComplete()
        }
    }

    @Transactional
    fun importCamt(inputStream: InputStream): String {
        val exportFileId = UUID.randomUUID().toString()
        processCamtStream(inputStream, exportFileId)
        return exportFileId
    }

    @Transactional
    fun importZip(inputStream: InputStream): String {
        val exportFileId = UUID.randomUUID().toString()
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                    logger.info("Found XML entry in ZIP: {}", entry.name)
                    processCamtStream(zipStream, exportFileId)
                }
                entry = zipStream.nextEntry
            }
        }
        return exportFileId
    }

    fun reEvaluatePending() {
        logger.info("Starting re-evaluation of pending transactions")
        val pendingDrafts = transactionDraftRepository.findByStatus(TransactionStatus.PENDING)
        if (pendingDrafts.isEmpty()) {
            logger.info("No pending drafts to re-evaluate")
            return
        }

        val payees = actualBudgetService.getPayees()
        logger.info("Fetched {} payees for re-evaluation", payees.size)

        val updatedDrafts =
            pendingDrafts.map { draft ->
                val updatedDraft = mappingEngineService.applyMappings(draft, payees)
                transactionDraftRepository.save(updatedDraft)
                updatedDraft
            }

        logger.info("Re-evaluation complete for {} drafts", updatedDrafts.size)
    }

    private fun processCamtStream(
        inputStream: InputStream,
        exportFileId: String,
    ) {
        logger.info("Processing CAMT stream with exportFileId: {}", exportFileId)
        val protectedStream = NonClosingInputStream(inputStream)
        val drafts = camtParserService.parse(protectedStream, exportFileId)
        logger.info("Parsed {} drafts from CAMT stream", drafts.size)

        transactionDraftRepository.saveAll(drafts)
    }
}
