package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.UUID

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
        logger.info("Starting CAMT import with exportFileId: {}", exportFileId)

        val drafts = camtParserService.parse(inputStream, exportFileId)
        logger.info("Parsed {} drafts from CAMT file", drafts.size)

        drafts.forEach { draft ->
            mappingEngineService.applyMappings(draft)
        }
        logger.info("Applied deterministic mappings to {} drafts", drafts.size)

        transactionDraftRepository.saveAll(drafts)
        logger.info("Saved {} drafts to repository", drafts.size)

        return exportFileId
    }
}
