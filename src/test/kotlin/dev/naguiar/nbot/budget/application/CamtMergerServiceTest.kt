package dev.naguiar.nbot.budget.application

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import com.prowidesoftware.swift.model.mx.dic.AccountStatement2
import com.prowidesoftware.swift.model.mx.dic.ActiveOrHistoricCurrencyAndAmount
import com.prowidesoftware.swift.model.mx.dic.BalanceType12
import com.prowidesoftware.swift.model.mx.dic.BalanceType12Code
import com.prowidesoftware.swift.model.mx.dic.BalanceType5Choice
import com.prowidesoftware.swift.model.mx.dic.BankToCustomerStatementV02
import com.prowidesoftware.swift.model.mx.dic.CashBalance3
import com.prowidesoftware.swift.model.mx.dic.CreditDebitCode
import com.prowidesoftware.swift.model.mx.dic.DateAndDateTimeChoice
import com.prowidesoftware.swift.model.mx.dic.ReportEntry2
import dev.naguiar.nbot.budget.domain.CamtFilter
import dev.naguiar.nbot.budget.domain.CamtFilterRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.math.BigDecimal
import java.time.LocalDate

class CamtMergerServiceTest {
    private val camtFilterRepository = mockk<CamtFilterRepository>()
    private val camtMergerService = CamtMergerService(camtFilterRepository)

    @Test
    fun `should match strictly when isStrict is true`() {
        // Given
        val preview =
            TransactionPreview(
                id = "1",
                date = LocalDate.now().atStartOfDay(),
                amount = BigDecimal.TEN,
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
            ReportEntry2().apply {
                addtlNtryInf = "/NAME/Secret Agent/IBAN/DE8899/EXTRA/INFO"
            }
        assertEquals("Secret Agent", camtMergerService.extractNameAndNotes(entry1).first)
        assertEquals("DE8899", camtMergerService.extractIban(entry1))

        // Case 2: Tags at the end of string (no trailing slash)
        val entry2 =
            ReportEntry2().apply {
                addtlNtryInf = "/TRTP/INFO/NAME/End Name"
            }
        assertEquals("End Name", camtMergerService.extractNameAndNotes(entry2).first)

        val entry3 =
            ReportEntry2().apply {
                addtlNtryInf = "/TRTP/INFO/IBAN/EndIBAN"
            }
        assertEquals("EndIBAN", camtMergerService.extractIban(entry3))

        // Case 3: Fallback to full string for Name
        val entry4 =
            ReportEntry2().apply {
                addtlNtryInf = "BEA, Card Payment at Store"
            }
        assertEquals("BEA, Card Payment at Store", camtMergerService.extractNameAndNotes(entry4).first)
        assertEquals(null, camtMergerService.extractIban(entry4))
    }

    @Test
    fun `should extract merchant name from BEA transactions and put technical info in notes`() {
        // Given
        val entry =
            ReportEntry2().apply {
                addtlNtryInf =
                    "BEA, Google Pay                 Parkeren ZGV                    NR:YY588R, 08.04.26/09:25       EDE GLD                         KAARTNUMMER: **0516"
            }

        // When
        val (name, notes) = camtMergerService.extractNameAndNotes(entry)

        // Then
        assertEquals("Parkeren ZGV", name)
        assertEquals("BEA, Google Pay, NR:YY588R, 08.04.26/09:25 EDE GLD KAARTNUMMER: **0516", notes)

        // Test with double spaces in merchant name (SumUp case)
        val entry2 =
            ReportEntry2().apply {
                addtlNtryInf =
                    "BEA, Betaalpas                  SumUp  *Hope Givers Fo          NR:MTXCYKQQ, 04.04.26/21:45     Amersfoort                      KAARTNUMMER: **0516"
            }
        val (name2, notes2) = camtMergerService.extractNameAndNotes(entry2)
        assertEquals("SumUp  *Hope Givers Fo", name2)
        assertEquals("BEA, Betaalpas, NR:MTXCYKQQ, 04.04.26/21:45 Amersfoort KAARTNUMMER: **0516", notes2)
    }

    @Test
    fun `should extract merchant name from GEA transactions and put technical info in notes`() {
        // Given
        val entry =
            ReportEntry2().apply {
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
    fun `mergeFromDocuments should merge multiple documents and apply exclusions`() {
        // Given
        val xmlContent = ClassPathResource("samples/camt053_sample.xml").inputStream.bufferedReader().readText()
        val doc = MxCamt05300102.parse(xmlContent)
        val documents = listOf(doc)

        every { camtFilterRepository.findAll() } returns emptyList()

        // Get IDs from preview first
        val previews = camtMergerService.getPreviewsFromDocuments(documents)
        val excludedId = previews.first().id

        // When
        val mergedXml = camtMergerService.mergeFromDocuments(documents, listOf(excludedId))

        // Then
        val mergedString = String(mergedXml)
        val ntryCount = "<Ntry>".toRegex().findAll(mergedString).count()
        assertEquals(2, ntryCount, "One entry should have been excluded")
        assertFalse(mergedString.contains(excludedId), "Excluded ID should not be in the XML")
    }

    @Test
    fun `should reflect cleanUpEntry changes in the merged XML`() {
        // Given
        val entry =
            ReportEntry2().apply {
                addtlNtryInf =
                    "BEA, Betaalpas                  SumUp  *Hope Givers Fo          NR:MTXCYKQQ, 04.04.26/21:45     Amersfoort                      KAARTNUMMER: **0516"
                amt =
                    ActiveOrHistoricCurrencyAndAmount().apply {
                        value = BigDecimal("10.00")
                        ccy = "EUR"
                    }
                cdtDbtInd = CreditDebitCode.DBIT
                bookgDt =
                    DateAndDateTimeChoice().apply {
                        dt = LocalDate.now()
                    }
            }
        val stmt =
            AccountStatement2().apply {
                ntry.add(entry)
                bal.add(
                    CashBalance3().apply {
                        tp =
                            BalanceType12().apply {
                                cdOrPrtry =
                                    BalanceType5Choice().apply {
                                        cd = BalanceType12Code.PRCD
                                    }
                            }
                        dt =
                            DateAndDateTimeChoice().apply {
                                dt = LocalDate.now()
                            }
                    },
                )
            }
        val doc =
            MxCamt05300102().apply {
                bkToCstmrStmt =
                    BankToCustomerStatementV02().apply {
                        this.stmt.add(stmt)
                    }
            }

        every { camtFilterRepository.findAll() } returns emptyList()

        // When - Preview performs cleanup
        camtMergerService.getPreviewsFromDocuments(listOf(doc))

        // Then - Document should be mutated
        assertEquals("SumUp  *Hope Givers Fo", entry.addtlNtryInf)

        // When - Merging
        val mergedXml = camtMergerService.mergeFromDocuments(listOf(doc))
        val mergedString = String(mergedXml)

        // Then - Merged XML should have clean name and structured notes
        assertTrue(mergedString.contains("<AddtlNtryInf>SumUp  *Hope Givers Fo</AddtlNtryInf>"))
        assertTrue(mergedString.contains("BEA, Betaalpas, NR:MTXCYKQQ, 04.04.26/21:45 Amersfoort KAARTNUMMER: **0516"))
    }

    @Test
    fun `should sort entries by date in merged XML`() {
        // Given
        val entryLate = createEntry("2026-05-30", "Late Entry")
        val entryEarly = createEntry("2026-05-01", "Early Entry")
        val entryMid = createEntry("2026-05-15", "Mid Entry")

        val statement =
            AccountStatement2().apply {
                ntry.addAll(listOf(entryLate, entryMid, entryEarly))
                bal.add(createBalance(BalanceType12Code.PRCD, LocalDate.parse("2026-05-01")))
                bal.add(createBalance(BalanceType12Code.CLBD, LocalDate.parse("2026-05-30")))
            }
        val doc =
            MxCamt05300102().apply {
                bkToCstmrStmt =
                    BankToCustomerStatementV02().apply {
                        stmt.add(statement)
                    }
            }

        every { camtFilterRepository.findAll() } returns emptyList()

        // When
        val mergedXml = camtMergerService.mergeFromDocuments(listOf(doc))
        val mergedString = String(mergedXml)

        // Then - Check order in XML
        val indices = listOf("Early Entry", "Mid Entry", "Late Entry").map { mergedString.indexOf(it) }
        assertEquals(indices.sorted(), indices, "Entries should be in chronological order in the XML")
    }

    @Test
    fun `should format SEPA transfer payee name as Name - IBAN`() {
        // Given
        val entry =
            ReportEntry2().apply {
                addtlNtryInf =
                    "/TRTP/SEPA OVERBOEKING/IBAN/NL99SPAR0123456789/BIC/SPARNL2A/NAME/JANE DOE/EREF/NOTPROVIDED"
            }

        // When
        val (name, _) = camtMergerService.extractNameAndNotes(entry)

        // Then
        assertEquals("JANE DOE - NL99SPAR0123456789", name)
    }

    private fun createEntry(
        dateStr: String,
        info: String,
    ): ReportEntry2 =
        ReportEntry2().apply {
            addtlNtryInf = info
            amt =
                ActiveOrHistoricCurrencyAndAmount().apply {
                    value = BigDecimal.TEN
                    ccy = "EUR"
                }
            cdtDbtInd = CreditDebitCode.DBIT
            bookgDt =
                DateAndDateTimeChoice().apply {
                    dt = LocalDate.parse(dateStr)
                }
        }

    private fun createBalance(
        typeCode: BalanceType12Code,
        date: LocalDate,
    ): CashBalance3 =
        CashBalance3().apply {
            tp =
                BalanceType12().apply {
                    cdOrPrtry =
                        BalanceType5Choice().apply {
                            cd = typeCode
                        }
                }
            dt =
                DateAndDateTimeChoice().apply {
                    dt = date
                }
        }
}
