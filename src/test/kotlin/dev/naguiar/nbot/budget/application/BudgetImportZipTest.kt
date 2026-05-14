package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BudgetImportZipTest {
    private val camtParserService = mockk<CamtParserService>()
    private val mappingEngineService = mockk<MappingEngineService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>()
    private val actualBudgetService = mockk<ActualBudgetService>(relaxed = true)
    private val budgetImportService =
        BudgetImportService(camtParserService, mappingEngineService, transactionDraftRepository, actualBudgetService)

    @Test
    fun `should process all XML files in ZIP recursively and use consistent exportFileId`() {
        val zipContent =
            ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zos ->
                    zos.putNextEntry(ZipEntry("file1.xml"))
                    zos.write("<xml1/>".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("sub/file2.xml"))
                    zos.write("<xml2/>".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("ignored.txt"))
                    zos.write("text".toByteArray())
                    zos.closeEntry()
                }
                baos.toByteArray()
            }

        val exportFileIdSlot = mutableListOf<String>()
        val mockDraft = mockk<TransactionDraft>()

        every { camtParserService.parse(any<InputStream>(), capture(exportFileIdSlot)) } answers {
            val stream = firstArg<InputStream>()
            stream.close() // Simulate parser closing the stream
            listOf(mockDraft)
        }
        every { mappingEngineService.applyMappings(mockDraft) } returns mockDraft
        every { transactionDraftRepository.saveAll(any<List<TransactionDraft>>()) } returns listOf(mockDraft)

        val returnedId = budgetImportService.importZip(zipContent.inputStream())

        // Verify consistent exportFileId
        assertEquals(2, exportFileIdSlot.size)
        assertEquals(returnedId, exportFileIdSlot[0])
        assertEquals(returnedId, exportFileIdSlot[1])

        // Verify processing and saving
        verify(exactly = 2) { camtParserService.parse(any<InputStream>(), any<String>()) }
        verify(exactly = 2) { mappingEngineService.applyMappings(mockDraft) }
        verify(exactly = 2) { transactionDraftRepository.saveAll(listOf(mockDraft)) }
    }
}
