package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import dev.naguiar.nbot.tools.socialschools.SocialSchoolsAlbumRepository
import dev.naguiar.nbot.tools.socialschools.SocialSchoolsClient
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SocialSchoolsControllerTest {
    private val client = mockk<SocialSchoolsClient>()
    private val repository = mockk<SocialSchoolsAlbumRepository>()
    private val properties = mockk<SocialSchoolsProperties>()

    private lateinit var controller: SocialSchoolsController
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        every { properties.downloadDir } returns null
        controller = SocialSchoolsController(client, repository, properties)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should return social schools view`() {
        every { repository.findAll() } returns emptyList()
        mockMvc
            .perform(get("/dashboard/socialschools"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/socialschools :: socialschools"))
    }
}
