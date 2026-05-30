package dev.naguiar.nbot.budget.application

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import com.prowidesoftware.swift.model.mx.dic.AccountStatement2
import com.prowidesoftware.swift.model.mx.dic.BalanceType12Code
import com.prowidesoftware.swift.model.mx.dic.BankToCustomerStatementV02
import com.prowidesoftware.swift.model.mx.dic.GroupHeader42
import com.prowidesoftware.swift.model.mx.dic.Pagination
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.zip.ZipInputStream

@Service
class CamtMergerService {
    private val logger = LoggerFactory.getLogger(CamtMergerService::class.java)

    fun mergeZip(inputStream: InputStream): ByteArray {
        val documents = parseAllFromZip(inputStream)
        require(documents.isNotEmpty()) { "ZIP contains no valid CAMT XML files" }

        val merged = merge(documents)

        // Remove 'camt:' namespace prefix to ensure compatibility with simpler parsers like Actual Budget
        val xmlString =
            merged
                .message()
                .replace("<camt:", "<")
                .replace("</camt:", "</")
                .replace("xmlns:camt=", "xmlns=")

        return xmlString.toByteArray(Charsets.UTF_8)
    }

    private fun parseAllFromZip(inputStream: InputStream): List<MxCamt05300102> {
        val documents = mutableListOf<MxCamt05300102>()
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                    logger.info("Parsing XML entry for merge: {}", entry.name)
                    val xml = NonClosingInputStream(zipStream).bufferedReader().readText()
                    documents.add(MxCamt05300102.parse(xml))
                }
                entry = zipStream.nextEntry
            }
        }
        return documents
    }

    private fun merge(documents: List<MxCamt05300102>): MxCamt05300102 {
        val allStatements =
            documents
                .flatMap { it.bkToCstmrStmt.stmt }
                .sortedBy { stmt -> stmtDate(stmt) }

        val allEntries = allStatements.flatMap { it.ntry }
        logger.info(
            "Merging {} entries from {} statements across {} files",
            allEntries.size,
            allStatements.size,
            documents.size,
        )

        val baseStmt = allStatements.first()
        val mergedBalances = pickMergedBalances(allStatements)

        val mergedStmt =
            AccountStatement2().apply {
                id = "merged-${UUID.randomUUID()}"
                elctrncSeqNb = baseStmt.elctrncSeqNb
                creDtTm = OffsetDateTime.now()
                acct = baseStmt.acct
                bal.addAll(mergedBalances)
                ntry.addAll(allEntries)
            }

        val grpHdr =
            GroupHeader42().apply {
                msgId = "merged-${UUID.randomUUID()}"
                creDtTm = OffsetDateTime.now()
                msgPgntn =
                    Pagination().apply {
                        pgNb = "1"
                        isLastPgInd = true
                    }
            }

        return MxCamt05300102().apply {
            bkToCstmrStmt =
                BankToCustomerStatementV02().apply {
                    this.grpHdr = grpHdr
                    stmt.add(mergedStmt)
                }
        }
    }

    private fun pickMergedBalances(sortedStatements: List<AccountStatement2>) =
        buildList {
            val earliest = sortedStatements.first()
            val latest = sortedStatements.last()

            earliest.bal
                .find { it.tp?.cdOrPrtry?.cd == BalanceType12Code.PRCD }
                ?.let(::add)
            latest.bal
                .find { it.tp?.cdOrPrtry?.cd == BalanceType12Code.CLBD }
                ?.let(::add)
        }

    private fun stmtDate(stmt: AccountStatement2): LocalDate =
        stmt.bal
            .firstOrNull()
            ?.dt
            ?.dt
            ?: stmt.creDtTm?.toLocalDate()
            ?: LocalDate.MAX
}
