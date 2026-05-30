package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.CamtFilter
import dev.naguiar.nbot.budget.domain.CamtFilterRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CamtMergerServiceTest {
    private val camtFilterRepository = mockk<CamtFilterRepository>()
    private val camtMergerService = CamtMergerService(camtFilterRepository)

    @Test
    fun `previewZip should extract transactions and apply filters`() {
        // Given
        val xmlContent = ClassPathResource("samples/camt053_sample.xml").inputStream.readBytes()
        val zipStream = ByteArrayOutputStream()
        ZipOutputStream(zipStream).use { zos ->
            zos.putNextEntry(ZipEntry("test.xml"))
            zos.write(xmlContent)
            zos.closeEntry()
        }
        val inputStream = zipStream.toByteArray().inputStream()

        val filter =
            CamtFilter(
                namePattern = "Coffee Shop",
                ibanPattern = null,
                isStrict = false,
            )
        every { camtFilterRepository.findAll() } returns listOf(filter)

        // When
        val previews = camtMergerService.previewZip(inputStream)

        // Then
        assertEquals(3, previews.size)

        // "Coffee Shop" should be filtered (from sample XML)
        val coffeeShop = previews.find { it.name == "Coffee Shop" }
        assertTrue(coffeeShop!!.isFiltered)
        assertEquals("Matched filter: Coffee Shop", coffeeShop.filterReason)

        // Others should not be filtered
        val supermarket = previews.find { it.name == "Supermarket XYZ" }
        assertFalse(supermarket!!.isFiltered)
    }

    @Test
    fun `should match strictly when isStrict is true`() {
        // Given
        val preview =
            TransactionPreview(
                id = "1",
                date =
                    java.time.LocalDate
                        .now()
                        .atStartOfDay(),
                amount = java.math.BigDecimal.TEN,
                currency = "EUR",
                name = "John Doe",
                iban = "NL1234",
            )

        // Strict match: both must match
        val filterStrict = CamtFilter(namePattern = "John", ibanPattern = "NL1234", isStrict = true)
        val filterStrictNoMatchName = CamtFilter(namePattern = "Jane", ibanPattern = "NL1234", isStrict = true)
        val filterStrictNoMatchIban = CamtFilter(namePattern = "John", ibanPattern = "NL5555", isStrict = true)

        // Non-strict match: either can match
        val filterNonStrict = CamtFilter(namePattern = "Jane", ibanPattern = "NL1234", isStrict = false)

        // Then - Strict Match
        val p1 = preview.copy()
        camtMergerService.applyFilters(p1, listOf(filterStrict))
        assertTrue(p1.isFiltered, "Should match when both name and IBAN match")

        val p2 = preview.copy()
        camtMergerService.applyFilters(p2, listOf(filterStrictNoMatchName))
        assertFalse(p2.isFiltered, "Should NOT match when name doesn't match in strict mode")

        val p3 = preview.copy()
        camtMergerService.applyFilters(p3, listOf(filterStrictNoMatchIban))
        assertFalse(p3.isFiltered, "Should NOT match when IBAN doesn't match in strict mode")

        // Then - Non-Strict Match
        val p4 = preview.copy()
        camtMergerService.applyFilters(p4, listOf(filterNonStrict))
        assertTrue(p4.isFiltered, "Should match when either name or IBAN matches in non-strict mode")
    }

    @Test
    fun `should extract name and IBAN from addtlNtryInf using regex with various formats`() {
        // Case 1: Standard tags with trailing slash
        val entry1 =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf = "/NAME/Secret Agent/IBAN/DE8899/EXTRA/INFO"
            }
        assertEquals("Secret Agent", camtMergerService.extractNameAndNotes(entry1).first)
        assertEquals("DE8899", camtMergerService.extractIban(entry1))

        // Case 2: Tags at the end of string (no trailing slash)
        val entry2 =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf = "/TRTP/INFO/NAME/End Name"
            }
        assertEquals("End Name", camtMergerService.extractNameAndNotes(entry2).first)

        val entry3 =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf = "/TRTP/INFO/IBAN/EndIBAN"
            }
        assertEquals("EndIBAN", camtMergerService.extractIban(entry3))

        // Case 3: Fallback to full string for Name
        val entry4 =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf = "BEA, Card Payment at Store"
            }
        assertEquals("BEA, Card Payment at Store", camtMergerService.extractNameAndNotes(entry4).first)
        assertEquals(null, camtMergerService.extractIban(entry4))
    }

    @Test
    fun `should extract merchant name from BEA transactions and put technical info in notes`() {
        // Given
        val entry =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf =
                    "BEA, Google Pay                 Parkeren ZGV                    NR:YY588R, 08.04.26/09:25       EDE GLD                         KAARTNUMMER: **0516"
            }

        // When
        val (name, notes) = camtMergerService.extractNameAndNotes(entry)

        // Then
        assertEquals("Parkeren ZGV", name)
        assertEquals("BEA, Google Pay, NR:YY588R, 08.04.26/09:25 EDE GLD KAARTNUMMER: **0516", notes)
    }

    @Test
    fun `should extract merchant name from GEA transactions and put technical info in notes`() {
        // Given
        val entry =
            com.prowidesoftware.swift.model.mx.dic.ReportEntry2().apply {
                addtlNtryInf =
                    "GEA, Betaalpas                  Geldmaat Scheepjesh 96          NR:813063, 20.04.26/13:24       Veenendaal                      KAARTNUMMER: **0516"
            }

        // When
        val (name, notes) = camtMergerService.extractNameAndNotes(entry)

        // Then
        assertEquals("Geldmaat Scheepjesh 96", name)
        assertEquals("GEA, Betaalpas, NR:813063, 20.04.26/13:24 Veenendaal KAARTNUMMER: **0516", notes)
    }

    @Test
    fun `mergeFromStrings should merge multiple XML strings and apply exclusions`() {
        // Given
        val xmlContent = ClassPathResource("samples/camt053_sample.xml").inputStream.bufferedReader().readText()
        val xmlStrings = listOf(xmlContent)

        every { camtFilterRepository.findAll() } returns emptyList()

        // Get IDs from preview first
        val previews = camtMergerService.getPreviewsFromStrings(xmlStrings)
        val excludedId = previews.first().id

        // When
        val mergedXml = camtMergerService.mergeFromStrings(xmlStrings, listOf(excludedId))

        // Then
        val mergedString = String(mergedXml)
        val ntryCount = "<Ntry>".toRegex().findAll(mergedString).count()
        assertEquals(2, ntryCount, "One entry should have been excluded")
        assertFalse(mergedString.contains(excludedId), "Excluded ID should not be in the XML")
    }
}
