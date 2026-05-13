package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.budget.application.ActualBudgetService
import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@Controller
class DashboardController(
    private val dataService: DashboardDataService,
    private val logEmitterService: SseLogEmitterService,
    private val budgetImportService: BudgetImportService,
    private val transactionDraftRepository: TransactionDraftRepository,
    private val actualBudgetService: ActualBudgetService,
    private val properties: ActualBudgetProperties
) {
    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        model.addAttribute("tools", dataService.getRegisteredTools())
        model.addAttribute("metrics", dataService.getMetrics())
        return "dashboard"
    }

    @GetMapping("/dashboard/metrics")
    fun metricsFragment(model: Model): String {
        model.addAttribute("metrics", dataService.getMetrics())
        return "fragments/metrics :: metrics"
    }

    @GetMapping("/dashboard/torrents")
    fun torrentsFragment(): String {
        return "fragments/logs :: logs"
    }

    @GetMapping("/dashboard/budget")
    fun budgetFragment(model: Model): String {
        model.addAttribute("drafts", transactionDraftRepository.findByStatus(TransactionStatus.PENDING))
        return "fragments/budget :: budget"
    }

    @PostMapping("/dashboard/budget/upload")
    fun uploadCamt(@RequestParam("file") file: MultipartFile, model: Model): String {
        if (!file.isEmpty) {
            val filename = file.originalFilename?.lowercase() ?: ""
            if (filename.endsWith(".zip")) {
                budgetImportService.importZip(file.inputStream)
            } else {
                budgetImportService.importCamt(file.inputStream)
            }
        }
        return budgetFragment(model)
    }

    @PostMapping("/dashboard/budget/approve/{id}")
    fun approveDraft(@PathVariable("id") id: UUID, model: Model): String {
        transactionDraftRepository.findById(id).ifPresent { draft ->
            draft.status = TransactionStatus.APPROVED
            transactionDraftRepository.save(draft)
        }
        return budgetFragment(model)
    }

    @PostMapping("/dashboard/budget/sync")
    fun syncBudget(model: Model): String {
        actualBudgetService.syncApprovedDrafts(properties.defaultAccountId)
        return budgetFragment(model)
    }

    @GetMapping("/dashboard/logs/stream")
    fun logStream(): SseEmitter {
        val emitter = SseEmitter(-1L)
        logEmitterService.addEmitter(emitter)
        return emitter
    }
}
