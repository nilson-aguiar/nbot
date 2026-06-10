package dev.naguiar.nbot.presentation.web

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class IndexControllerTest {
    private val controller = IndexController()
    private val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    @Test
    fun `should redirect from root to dashboard`() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/dashboard"))
    }
}
