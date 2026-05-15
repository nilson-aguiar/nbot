package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.PayeeMappingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LearningService(
    private val actualBudgetService: ActualBudgetService,
    private val payeeMappingRepository: PayeeMappingRepository,
) {
    private val log = LoggerFactory.getLogger(LearningService::class.java)

    fun learnFromActualBudget() {
        log.info("Starting learning process from Actual Budget")
        // TODO: Implement learning logic from Actual Budget transactions
        log.info("Learning service is ready for future expansion")
    }
}
