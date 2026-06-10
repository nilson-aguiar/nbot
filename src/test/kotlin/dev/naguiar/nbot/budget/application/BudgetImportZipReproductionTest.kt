package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraftRepository
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.io.FileInputStream

@Disabled("Reproduction test for a specific sample. Use manually when needed.")
@SpringBootTest
@ActiveProfiles("test")
class BudgetImportZipReproductionTest {
    @Autowired
    private lateinit var budgetImportService: BudgetImportService

    @Autowired
    private lateinit var transactionDraftRepository: TransactionDraftRepository

    @Test
    fun `should import specific sample zip file`() {
        val zipFile = File("sample/2047164370_105857009_070426000000.zip")
        if (!zipFile.exists()) {
            println("Sample file not found at ${zipFile.absolutePath}")
            return
        }

        FileInputStream(zipFile).use { inputStream ->
            budgetImportService.importZip(inputStream)
        }
    }
}
