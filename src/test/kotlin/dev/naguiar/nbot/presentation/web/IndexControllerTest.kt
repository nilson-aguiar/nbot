package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.HomepageData
import dev.naguiar.nbot.application.web.HomepageService
import dev.naguiar.nbot.application.web.NamespaceGroup
import dev.naguiar.nbot.application.web.ServiceCard
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

class IndexControllerTest {
    private val homepageService = mockk<HomepageService>()
    private val controller = IndexController(homepageService)
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
    fun `should render homepage at root using X-Forwarded-Host`() {
        val sampleData =
            HomepageData(
                groups =
                    listOf(
                        NamespaceGroup(
                            namespace = "media-flix",
                            services =
                                listOf(
                                    ServiceCard(
                                        name = "sonarr",
                                        namespace = "media-flix",
                                        primaryHost = "sonarr.naguiar.dev",
                                        url = "https://sonarr.naguiar.dev",
                                        tls = true,
                                        secondaryHosts = listOf("sonarr.local.naguiar.dev"),
                                    ),
                                ),
                        ),
                    ),
                totalServices = 1,
            )
        every { homepageService.getHomepageData("nbot.naguiar.dev", false) } returns sampleData

        mockMvc
            .perform(get("/").header("X-Forwarded-Host", "nbot.naguiar.dev"))
            .andExpect(status().isOk)
            .andExpect(view().name("homepage"))
            .andExpect(model().attribute("homepage", sampleData))
    }

    @Test
    fun `should refresh homepage services fragment`() {
        val sampleData = HomepageData(groups = emptyList(), totalServices = 0)
        every { homepageService.getHomepageData("localhost", true) } returns sampleData

        mockMvc
            .perform(post("/homepage/refresh"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/homepage-services :: services"))
            .andExpect(model().attribute("homepage", sampleData))
    }
}
