package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.SseBudgetEmitterService
import dev.naguiar.nbot.budget.application.ActualBudgetService
import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.view.InternalResourceViewResolver
import org.thymeleaf.TemplateEngine

class BudgetControllerTest {
    private val budgetImportService = mockk<BudgetImportService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>()
    private val actualBudgetService = mockk<ActualBudgetService>()
    private val properties = ActualBudgetProperties(defaultAccountId = "test-account")
    private val templateEngine = mockk<TemplateEngine>()
    private val sseBudgetEmitterService = mockk<SseBudgetEmitterService>(relaxed = true)
    private val controller =
        BudgetController(
            budgetImportService,
            transactionDraftRepository,
            actualBudgetService,
            properties,
            templateEngine,
            sseBudgetEmitterService,
        )
    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(controller)
            .setViewResolvers(
                InternalResourceViewResolver().apply {
                    setPrefix("/templates/")
                    setSuffix(".html")
                },
            ).build()

    @Test
    fun `should return budget fragment`() {
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(get("/dashboard/budget"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))
    }

    @Test
    fun `should upload camt xml and return budget fragment`() {
        val file = MockMultipartFile("file", "test.xml", "text/xml", "<xml></xml>".toByteArray())
        every { budgetImportService.importCamt(any()) } returns "test-id"
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(multipart("/dashboard/budget/upload").file(file))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))

        verify { budgetImportService.importCamt(any()) }
    }

    @Test
    fun `should upload zip and return budget fragment`() {
        val file = MockMultipartFile("file", "test.zip", "application/zip", "dummy zip".toByteArray())
        every { budgetImportService.importZip(any()) } returns "test-id"
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(multipart("/dashboard/budget/upload").file(file))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))

        verify { budgetImportService.importZip(any()) }
    }

    @Test
    fun `should approve draft and return budget fragment`() {
        val id = UUID.randomUUID()
        every { transactionDraftRepository.findById(id) } returns mockk(relaxed = true)
        every { transactionDraftRepository.save(any()) } returns mockk()
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(post("/dashboard/budget/approve/$id"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))

        verify { transactionDraftRepository.save(any()) }
    }

    @Test
    fun `should sync budget and return budget fragment`() {
        every { actualBudgetService.syncApprovedDrafts("test-account") } returns Unit
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(post("/dashboard/budget/sync"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))
    }

    @Test
    fun `should trigger async re-evaluation and return fragment`() {
        val draftIds = listOf(UUID.randomUUID())
        every { budgetImportService.reEvaluateAsync(any(), any(), any()) } returns Unit

        mockMvc
            .perform(
                post("/dashboard/budget/re-evaluate")
                    .param("draftIds", draftIds[0].toString()),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: reEvaluateButton"))

        verify { budgetImportService.reEvaluateAsync(eq(draftIds), any(), any()) }
    }

    @Test
    fun `should return reEvaluateButton if draftIds is empty`() {
        mockMvc
            .perform(post("/dashboard/budget/re-evaluate"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: reEvaluateButton"))
    }

    @Test
    fun `should return budget stream emitter`() {
        mockMvc
            .perform(get("/dashboard/budget/stream"))
            .andExpect(status().isOk)
    }
}
