package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import dev.naguiar.nbot.budget.infrastructure.db.PayeeMappingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MappingEngineService(
    private val payeeMappingRepository: PayeeMappingRepository,
    private val budgetAiService: BudgetAiService,
    private val properties: ActualBudgetProperties
) {
    private val log = LoggerFactory.getLogger(MappingEngineService::class.java)

    fun applyMappings(draft: TransactionDraft, payees: List<dev.naguiar.nbot.budget.infrastructure.api.ActualPayee> = emptyList()) {
        val mappings = payeeMappingRepository.findAll()
        
        for (mapping in mappings) {
            val regex = Regex(mapping.bankPattern, RegexOption.IGNORE_CASE)
            
            if (regex.containsMatchIn(draft.bankPayeeName) || regex.containsMatchIn(draft.bankDescription)) {
                draft.suggestedPayeeId = mapping.actualPayeeId
                draft.suggestedPayeeName = mapping.actualPayeeName
                return
            }
        }

        // AI Fallback
        try {
            val internalAccounts = properties.internalAccounts.map { 
                BudgetAiService.InternalAccount(it, it)
            }
            
            val knownPayees = payees.map { 
                BudgetAiService.ActualPayee(it.id, it.name)
            }
            
            val aiResponse = budgetAiService.suggestMapping(
                bankPayeeName = draft.bankPayeeName,
                bankDescription = draft.bankDescription,
                knownPayees = knownPayees,
                internalAccounts = internalAccounts
            )

            if (aiResponse != null) {
                if (aiResponse.isTransfer && aiResponse.confidence > 0.7) {
                    log.info("AI identified transaction as internal transfer: ${aiResponse.reasoning}")
                    draft.status = TransactionStatus.IGNORED
                } else if (aiResponse.payeeId != null && aiResponse.confidence > 0.7) {
                    log.info("AI suggested mapping: ${aiResponse.payeeName} (confidence: ${aiResponse.confidence})")
                    draft.suggestedPayeeId = aiResponse.payeeId
                    draft.suggestedPayeeName = aiResponse.payeeName
                }
            }
        } catch (e: Exception) {
            log.error("AI mapping suggestion failed", e)
        }
    }
}
