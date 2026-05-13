package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import dev.naguiar.nbot.budget.infrastructure.db.TransactionDraftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BudgetImportZipTest {
    private val camtParserService = mockk<CamtParserService>()
    private val mappingEngineService = mockk<MappingEngineService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>()
    private val budgetImportService = BudgetImportService(camtParserService, mappingEngineService, transactionDraftRepository)

    @Test
    fun `should process all XML files in ZIP recursively`() {
        val zipContent = ByteArrayOutputStream().use { baos ->
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

        every { camtParserService.parse(any<InputStream>(), any<String>()) } returns emptyList<TransactionDraft>()
        every { transactionDraftRepository.saveAll(any<List<TransactionDraft>>()) } returns emptyList<TransactionDraft>()

        budgetImportService.importZip(zipContent.inputStream())

        verify(exactly = 2) { camtParserService.parse(any<InputStream>(), any<String>()) }
    }
}
