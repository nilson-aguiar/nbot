package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.SseBudgetEmitterService
import dev.naguiar.nbot.budget.application.ActualBudgetService
import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.*

@RequestMapping("/dashboard/budget")
@Controller
class BudgetController(
    private val budgetImportService: BudgetImportService,
    private val transactionDraftRepository: TransactionDraftRepository,
    private val actualBudgetService: ActualBudgetService,
    private val properties: ActualBudgetProperties,
    private val templateEngine: TemplateEngine,
    private val sseBudgetEmitterService: SseBudgetEmitterService,
) {
    @GetMapping
    fun budgetFragment(model: Model): String {
        model.addAttribute("drafts", transactionDraftRepository.findByStatus(TransactionStatus.PENDING))
        return "fragments/budget :: budget"
    }

    @PostMapping("/upload")
    fun uploadCamt(
        @RequestParam("file") file: MultipartFile,
        model: Model,
    ): String {
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

    @PostMapping("/approve/{id}")
    fun approveDraft(
        @PathVariable id: UUID,
        model: Model,
    ): String {
        transactionDraftRepository.findById(id)?.let { draft ->
            val approvedDraft = draft.copy(status = TransactionStatus.APPROVED)
            transactionDraftRepository.save(approvedDraft)
        }
        return budgetFragment(model)
    }

    @PostMapping("/sync")
    fun syncBudget(model: Model): String {
        actualBudgetService.syncApprovedDrafts(properties.defaultAccountId)
        return budgetFragment(model)
    }

    @PostMapping("/re-evaluate")
    fun reEvaluateBudget(
        @RequestParam(required = false) draftIds: List<UUID>?,
    ): String {
        if (draftIds.isNullOrEmpty()) {
            return "fragments/budget :: reEvaluateButton"
        }

        budgetImportService.reEvaluateAsync(
            draftIds = draftIds,
            onProgress = { draft ->
                val context =
                    Context().apply {
                        setVariable("draft", draft)
                        setVariable("oob", true)
                    }
                val html = templateEngine.process("fragments/draft-row :: draftRow", context)
                sseBudgetEmitterService.broadcast(html)
            },
            onComplete = {
                val context =
                    Context()
                        .apply { setVariable("oob", true) }
                val buttonHtml = templateEngine.process("fragments/budget :: reEvaluateButton", context)

                val pendingDrafts = transactionDraftRepository.findByStatus(TransactionStatus.PENDING)
                context.setVariable("drafts", pendingDrafts)
                val badgeHtml = templateEngine.process("fragments/budget :: pendingBadge", context)

                sseBudgetEmitterService.broadcast(buttonHtml + badgeHtml)
            },
        )

        return "fragments/budget :: reEvaluateButton"
    }

    @GetMapping("/stream")
    fun budgetStream(): SseEmitter {
        val emitter = SseEmitter(-1L)
        sseBudgetEmitterService.addEmitter(emitter)
        return emitter
    }
}
