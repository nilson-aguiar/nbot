package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.domain.TransactionDraft
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Service
class CamtParserService {
    fun parse(
        inputStream: InputStream,
        exportFileId: String,
    ): List<TransactionDraft> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false // Simplifies XPath if we don't care about namespaces
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(inputStream)

        val xpath = XPathFactory.newInstance().newXPath()
        val entryNodes = xpath.evaluate("//Ntry", doc, XPathConstants.NODESET) as NodeList

        val transactions = mutableListOf<TransactionDraft>()

        for (i in 0 until entryNodes.length) {
            val entryNode = entryNodes.item(i)

            val amountStr = xpath.evaluate("Amt", entryNode)
            val currency = xpath.evaluate("Amt/@Ccy", entryNode)
            val indicator = xpath.evaluate("CdtDbtInd", entryNode)

            var bookingDateStr = xpath.evaluate("BookgDt/Dt", entryNode)
            if (bookingDateStr.isNullOrBlank()) {
                bookingDateStr = xpath.evaluate("BookgDt/DtTm", entryNode)
            }
            if (bookingDateStr.isNullOrBlank()) {
                // Fallback to value date if booking date is missing
                bookingDateStr = xpath.evaluate("ValDt/Dt", entryNode)
            }
            if (bookingDateStr.isNullOrBlank()) {
                bookingDateStr = xpath.evaluate("ValDt/DtTm", entryNode)
            }

            // Try to find payee name in Cdtr or Dbtr
            var payeeName = xpath.evaluate("NtryDtls/TxDtls/RltdPties/Cdtr/Nm", entryNode)
            if (payeeName.isNullOrBlank()) {
                payeeName = xpath.evaluate("NtryDtls/TxDtls/RltdPties/Dbtr/Nm", entryNode)
            }
            if (payeeName.isNullOrBlank()) {
                // Try outside TxDtls
                payeeName = xpath.evaluate("RltdPties/Cdtr/Nm", entryNode)
            }
            if (payeeName.isNullOrBlank()) {
                payeeName = xpath.evaluate("RltdPties/Dbtr/Nm", entryNode)
            }

            val description = xpath.evaluate("NtryDtls/TxDtls/RmtInf/Ustrd", entryNode)

            val amount = if (amountStr.isNotBlank()) BigDecimal(amountStr) else BigDecimal.ZERO
            val cents = amount.movePointRight(2).toLong()
            val finalAmount = if (indicator == "DBIT") -cents else cents

            val date =
                try {
                    if (bookingDateStr.length >= 10) {
                        LocalDate.parse(bookingDateStr.substring(0, 10))
                    } else {
                        LocalDate.now()
                    }
                } catch (e: Exception) {
                    LocalDate.now()
                }

            transactions.add(
                TransactionDraft(
                    bookingDate = date,
                    amount = finalAmount,
                    currency = currency,
                    bankPayeeName = payeeName ?: "",
                    bankDescription = description ?: "",
                    exportFileId = exportFileId,
                ),
            )
        }

        return transactions
    }
}
