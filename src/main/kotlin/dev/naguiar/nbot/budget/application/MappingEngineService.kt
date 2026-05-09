package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.infrastructure.db.PayeeMappingRepository
import org.springframework.stereotype.Service

@Service
class MappingEngineService(
    private val payeeMappingRepository: PayeeMappingRepository
) {

    fun map(draft: TransactionDraft) {
        val mappings = payeeMappingRepository.findAll()
        
        for (mapping in mappings) {
            val regex = Regex(mapping.bankPattern, RegexOption.IGNORE_CASE)
            
            if (regex.containsMatchIn(draft.bankPayeeName) || regex.containsMatchIn(draft.bankDescription)) {
                draft.suggestedPayeeId = mapping.actualPayeeId
                draft.suggestedPayeeName = mapping.actualPayeeName
                return
            }
        }
    }
}
