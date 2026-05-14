package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*
import java.util.zip.ZipInputStream

@Service
class BudgetImportService(
    private val camtParserService: CamtParserService,
    private val mappingEngineService: MappingEngineService,
    private val transactionDraftRepository: TransactionDraftRepository,
    private val actualBudgetService: ActualBudgetService,
) {
    private val logger = LoggerFactory.getLogger(BudgetImportService::class.java)

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

/**
 * A wrapper for InputStream that ignores calls to close().
 * This is useful when passing a ZipInputStream to a consumer that might close it
 * (like an XML parser), which would prevent reading further entries from the ZIP.
 */
private class NonClosingInputStream(
    private val delegate: InputStream,
) : InputStream() {
    override fun read(): Int = delegate.read()

    override fun read(b: ByteArray): Int = delegate.read(b)

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int = delegate.read(b, off, len)

    override fun skip(n: Long): Long = delegate.skip(n)

    override fun available(): Int = delegate.available()

    override fun mark(readlimit: Int) = delegate.mark(readlimit)

    override fun reset() = delegate.reset()

    override fun markSupported(): Boolean = delegate.markSupported()

    override fun close() {
        // Do nothing
    }
}
