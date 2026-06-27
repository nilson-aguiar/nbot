package dev.naguiar.nbot.presentation.web

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import dev.naguiar.nbot.budget.application.CamtMergerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
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
        val documents = listOf(mockk<MxCamt05300102>())
        every { camtMergerService.parseZipToDocuments(any()) } returns documents
        every { camtMergerService.getPreviewsFromDocuments(documents) } returns emptyList()

        mockMvc
            .perform(
                multipart("/dashboard/tools/merge-preview")
                    .file(file),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("previews"))

        verify { camtMergerService.parseZipToDocuments(any()) }
        verify { camtMergerService.getPreviewsFromDocuments(documents) }
    }

    @Test
    fun `should save filter and return preview`() {
        val documents = listOf(mockk<MxCamt05300102>())
        every { camtMergerService.saveFilter(any()) } returns mockk()
        every { camtMergerService.getPreviewsFromDocuments(documents) } returns emptyList()

        mockMvc
            .perform(
                post("/dashboard/tools/filters")
                    .param("namePattern", "test")
                    .param("ibanPattern", "DE123")
                    .param("isStrict", "true")
                    .sessionAttr("mergePreviewDocs", documents),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("filterSuccess"))

        verify { camtMergerService.saveFilter(any()) }
    }

    @Test
    fun `should delete filter and return preview`() {
        val id = UUID.randomUUID()
        val documents = listOf(mockk<MxCamt05300102>())
        every { camtMergerService.deleteFilter(id) } returns Unit
        every { camtMergerService.getPreviewsFromDocuments(documents) } returns emptyList()

        mockMvc
            .perform(
                post("/dashboard/tools/filters/delete/$id")
                    .sessionAttr("mergePreviewDocs", documents),
            ).andExpect(status().isOk)
            .andExpect(view().name("fragments/tools :: preview"))
            .andExpect(model().attributeExists("filterDeleted"))

        verify { camtMergerService.deleteFilter(id) }
    }

    @Test
    fun `should merge xml and return byte array`() {
        val documents = listOf(mockk<MxCamt05300102>())
        every { camtMergerService.mergeFromDocuments(documents, any()) } returns "merged".toByteArray()

        mockMvc
            .perform(
                post("/dashboard/tools/merge-xml")
                    .sessionAttr("mergePreviewDocs", documents)
                    .param("excludedIds", "id1", "id2"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"merged-camt.xml\""))
            .andExpect(content().contentType("application/xml"))
            .andExpect(content().bytes("merged".toByteArray()))

        verify { camtMergerService.mergeFromDocuments(documents, listOf("id1", "id2")) }
    }
}
