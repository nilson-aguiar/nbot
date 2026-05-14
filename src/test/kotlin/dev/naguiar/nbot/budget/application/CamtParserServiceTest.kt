package dev.naguiar.nbot.budget.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate

class CamtParserServiceTest {
    private val camtParserService = CamtParserService()

    @Test
    fun `should parse camt053 xml correctly`() {
        val xmlStream = ClassPathResource("samples/camt053_sample.xml").inputStream
        val transactions = camtParserService.parse(xmlStream, "test-file-id")

        assertEquals(3, transactions.size)

        // First transaction (DBIT)
        val t1 = transactions[0]
        assertEquals(LocalDate.of(2023, 10, 27), t1.bookingDate)
        assertEquals(-1234L, t1.amount) // -12.34 EUR in cents
        assertEquals("EUR", t1.currency)
        assertEquals("Supermarket XYZ", t1.bankPayeeName)
        assertEquals("Grocery shopping", t1.bankDescription)
        assertEquals("Entry info for supermarket", t1.bankAdditionalInfo)
        assertEquals("test-file-id", t1.exportFileId)

        // Second transaction (CRDT)
        val t2 = transactions[1]
        assertEquals(LocalDate.of(2023, 10, 28), t2.bookingDate)
        assertEquals(100000L, t2.amount) // 1000.00 EUR in cents
        assertEquals("EUR", t2.currency)
        assertEquals("Employer ABC", t2.bankPayeeName)
        assertEquals("Monthly Salary", t2.bankDescription)
        assertEquals(null, t2.bankAdditionalInfo)
        assertEquals("test-file-id", t2.exportFileId)

        // Third transaction (DBIT with DtTm and RltdPties outside TxDtls)
        val t3 = transactions[2]
        assertEquals(LocalDate.of(2023, 10, 29), t3.bookingDate)
        assertEquals(-550L, t3.amount)
        assertEquals("Coffee Shop", t3.bankPayeeName)
        assertEquals("Coffee break", t3.bankAdditionalInfo)
    }
}
