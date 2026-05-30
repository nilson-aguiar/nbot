package dev.naguiar.nbot.budget.application

import org.junit.jupiter.api.Assertions.assertNotNull
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
class CamtMergerReproductionTest {
    @Autowired
    private lateinit var camtMergerService: CamtMergerService

    @Test
    fun `should merge xml from sample zip file`() {
        val zipFile = File("sample/2047164370_105857009_070426000000.zip")
        if (!zipFile.exists()) {
            println("Sample file not found at ${zipFile.absolutePath}")
            return
        }

        val xmlStrings =
            FileInputStream(zipFile).use { inputStream ->
                camtMergerService.parseZipToStrings(inputStream)
            }

        println("Found ${xmlStrings.size} XML strings in ZIP")

        val mergedXml = camtMergerService.mergeFromStrings(xmlStrings)
        assertNotNull(mergedXml)
        println("Merged XML size: ${mergedXml.size} bytes")
    }
}
