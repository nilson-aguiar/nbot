package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.application.web.HomepageData
import dev.naguiar.nbot.application.web.HomepageService
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.view.InternalResourceViewResolver

class DashboardControllerTest {
    private val dataService = mockk<DashboardDataService>()
    private val homepageService = mockk<HomepageService>()
    private val logEmitterService = mockk<SseLogEmitterService>(relaxed = true)
    private val controller =
        DashboardController(
            dataService,
            homepageService,
            logEmitterService,
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
    fun `should return dashboard view`() {
        every { dataService.getRegisteredTools() } returns emptyList()
        every { homepageService.getHomepageData("localhost") } returns HomepageData(emptyList(), 0)

        mockMvc
            .perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("tools", "homepage"))
    }

    @Test
    fun `should return torrents fragment`() {
        mockMvc
            .perform(get("/dashboard/torrents"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/logs :: logs"))
    }

    @Test
    fun `should return log stream emitter`() {
        mockMvc
            .perform(get("/dashboard/logs/stream"))
            .andExpect(status().isOk)
    }
}
