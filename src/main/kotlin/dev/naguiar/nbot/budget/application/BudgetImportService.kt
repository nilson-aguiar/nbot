package dev.naguiar.nbot.budget.application

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
                    val xml = NonClosingInputStream(zipStream).bufferedReader().readText()
                    if (parseCamt053(xml) != null) {
                        logger.info("Found valid CAMT.053 entry in ZIP: {}", entry.name)
                        processCamtStream(xml.byteInputStream(), exportFileId)
                    } else {
                        logger.warn("Skipping file in ZIP as it's not a valid CAMT.053 XML: {}", entry.name)
                    }
                }
                entry = zipStream.nextEntry
            }
        }
        return exportFileId
    }

    private fun parseCamt053(xml: String): MxCamt05300102? =
        try {
            MxCamt05300102.parse(xml)
        } catch (_: Exception) {
            null
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
