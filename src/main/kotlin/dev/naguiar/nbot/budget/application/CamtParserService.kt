package dev.naguiar.nbot.budget.application

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import com.prowidesoftware.swift.model.mx.dic.CreditDebitCode
import com.prowidesoftware.swift.model.mx.dic.ReportEntry2
import dev.naguiar.nbot.budget.domain.TransactionDraft
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDate

@Service
class CamtParserService {
    fun parse(
        inputStream: InputStream,
        exportFileId: String,
    ): List<TransactionDraft> {
        val mx = MxCamt05300102.parse(inputStream.bufferedReader().readText())
        val transactions = mutableListOf<TransactionDraft>()

        mx.bkToCstmrStmt.stmt.forEach { stmt ->
            stmt.ntry.forEach { entry ->
                val bookingDate = findBookingDate(entry)
                val amount = entry.amt.value
                val cents = amount.movePointRight(2).toLong()
                val finalAmount = if (entry.cdtDbtInd == CreditDebitCode.DBIT) -cents else cents

                val payeeName = findPayeeName(entry)
                val description = findDescription(entry)

                transactions.add(
                    TransactionDraft(
                        bookingDate = bookingDate,
                        amount = finalAmount,
                        currency = entry.amt.ccy,
                        bankPayeeName = payeeName ?: "",
                        bankDescription = description ?: "",
                        exportFileId = exportFileId,
                    ),
                )
            }
        }

        return transactions
    }

    private fun findBookingDate(entry: ReportEntry2): LocalDate =
        entry.bookgDt?.dt ?: entry.bookgDt?.dtTm?.toLocalDate()
            ?: entry.valDt?.dt ?: entry.valDt?.dtTm?.toLocalDate()
            ?: LocalDate.now()

    private fun findPayeeName(entry: ReportEntry2): String? {
        entry.ntryDtls?.forEach { details ->
            details.txDtls?.forEach { tx ->
                tx.rltdPties?.let { parties ->
                    parties.cdtr?.nm?.let { return it }
                    parties.dbtr?.nm?.let { return it }
                }
            }
        }

        return entry.addtlNtryInf
    }

    private fun findDescription(entry: ReportEntry2): String? =
        entry.ntryDtls
            ?.firstOrNull()
            ?.txDtls
            ?.firstOrNull()
            ?.rmtInf
            ?.ustrd
            ?.firstOrNull()
}
