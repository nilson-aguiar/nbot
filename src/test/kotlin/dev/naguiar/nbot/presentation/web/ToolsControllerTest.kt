package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.budget.application.CamtMergerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.view.InternalResourceViewResolver
import java.util.*

class ToolsControllerTest {
    private val camtMergerService = mockk<CamtMergerService>()
    private val controller = ToolsController(camtMergerService)
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
    fun `should return tools fragment`() {
        mockMvc
            .perform(get("/dashboard/tools"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: tools"))
    }

    @Test
    fun `should handle merge preview`() {
        val file = MockMultipartFile("file", "test.zip", "application/zip", "dummy content".toByteArray())
        val xmlStrings = listOf("<xml1/>", "<xml2/>")
        every { camtMergerService.parseZipToStrings(any()) } returns xmlStrings
        every { camtMergerService.getPreviewsFromStrings(xmlStrings) } returns emptyList()

        mockMvc
            .perform(
                multipart("/dashboard/tools/merge-preview")
                    .file(file),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("previews"))

        verify { camtMergerService.parseZipToStrings(any()) }
        verify { camtMergerService.getPreviewsFromStrings(xmlStrings) }
    }

    @Test
    fun `should save filter and return preview`() {
        val xmlStrings = listOf("<xml/>")
        every { camtMergerService.saveFilter(any()) } returns mockk()
        every { camtMergerService.getPreviewsFromStrings(xmlStrings) } returns emptyList()

        mockMvc
            .perform(
                post("/dashboard/tools/filters")
                    .param("namePattern", "test")
                    .param("ibanPattern", "DE123")
                    .param("isStrict", "true")
                    .sessionAttr("mergePreviewXmls", xmlStrings),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("filterSuccess"))

        verify { camtMergerService.saveFilter(any()) }
    }

    @Test
    fun `should delete filter and return preview`() {
        val id = UUID.randomUUID()
        val xmlStrings = listOf("<xml/>")
        every { camtMergerService.deleteFilter(id) } returns Unit
        every { camtMergerService.getPreviewsFromStrings(xmlStrings) } returns emptyList()

        mockMvc
            .perform(
                post("/dashboard/tools/filters/delete/$id")
                    .sessionAttr("mergePreviewXmls", xmlStrings),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("filterDeleted"))

        verify { camtMergerService.deleteFilter(id) }
    }

    @Test
    fun `should merge xml and return byte array`() {
        val xmlStrings = listOf("<xml/>")
        every { camtMergerService.mergeFromStrings(xmlStrings, any()) } returns "merged".toByteArray()

        mockMvc
            .perform(
                post("/dashboard/tools/merge-xml")
                    .sessionAttr("mergePreviewXmls", xmlStrings)
                    .param("excludedIds", "id1", "id2"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"merged-camt.xml\""))
            .andExpect(content().contentType("application/xml"))
            .andExpect(content().bytes("merged".toByteArray()))

        verify { camtMergerService.mergeFromStrings(xmlStrings, listOf("id1", "id2")) }
    }
}
