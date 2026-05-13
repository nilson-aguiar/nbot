package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

@Service
class BudgetImportService(
    private val camtParserService: CamtParserService,
    private val mappingEngineService: MappingEngineService,
    private val transactionDraftRepository: TransactionDraftRepository
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

    private fun processCamtStream(inputStream: InputStream, exportFileId: String) {
        logger.info("Processing CAMT stream with exportFileId: {}", exportFileId)
        val drafts = camtParserService.parse(inputStream, exportFileId)
        logger.info("Parsed {} drafts from CAMT stream", drafts.size)

        drafts.forEach { draft ->
            mappingEngineService.applyMappings(draft)
        }
        transactionDraftRepository.saveAll(drafts)
        logger.info("Saved {} drafts to repository", drafts.size)
    }
}
