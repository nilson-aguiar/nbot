package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.application.web.MetricsInfo
import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.budget.domain.TransactionStatus
import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.view.InternalResourceViewResolver

class DashboardControllerTest {
    private val dataService = mockk<DashboardDataService>()
    private val logEmitterService = mockk<SseLogEmitterService>(relaxed = true)
    private val budgetImportService = mockk<BudgetImportService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>()
    private val controller = DashboardController(dataService, logEmitterService, budgetImportService, transactionDraftRepository)
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
    fun `should return dashboard view`() {
        every { dataService.getRegisteredTools() } returns emptyList()
        every { dataService.getMetrics() } returns MetricsInfo(10, 300)

        mockMvc
            .perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("tools", "metrics"))
    }

    @Test
    fun `should return metrics fragment`() {
        every { dataService.getMetrics() } returns MetricsInfo(10, 300)

        mockMvc
            .perform(get("/dashboard/metrics"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/metrics :: metrics"))
            .andExpect(model().attributeExists("metrics"))
    }

    @Test
    fun `should return torrents fragment`() {
        mockMvc
            .perform(get("/dashboard/torrents"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/logs :: logs"))
    }

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
    fun `should return log stream emitter`() {
        mockMvc
            .perform(get("/dashboard/logs/stream"))
            .andExpect(status().isOk)
    }
}
