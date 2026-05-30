package dev.naguiar.nbot.budget.application

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import com.prowidesoftware.swift.model.mx.dic.AccountStatement2
import com.prowidesoftware.swift.model.mx.dic.BalanceType12Code
import com.prowidesoftware.swift.model.mx.dic.BankToCustomerStatementV02
import com.prowidesoftware.swift.model.mx.dic.EntryDetails1
import com.prowidesoftware.swift.model.mx.dic.EntryTransaction2
import com.prowidesoftware.swift.model.mx.dic.GroupHeader42
import com.prowidesoftware.swift.model.mx.dic.Pagination
import com.prowidesoftware.swift.model.mx.dic.RemittanceInformation5
import com.prowidesoftware.swift.model.mx.dic.ReportEntry2
import dev.naguiar.nbot.budget.domain.CamtFilter
import dev.naguiar.nbot.budget.domain.CamtFilterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import java.util.zip.ZipInputStream

@Service
class CamtMergerService(
    private val camtFilterRepository: CamtFilterRepository,
) {
    private val logger = LoggerFactory.getLogger(CamtMergerService::class.java)

    fun parseZipToDocuments(inputStream: InputStream): List<MxCamt05300102> {
        val documents = mutableListOf<MxCamt05300102>()
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                    val xml = NonClosingInputStream(zipStream).bufferedReader().readText()
                    parseCamt053(xml)?.let { documents.add(it) }
                        ?: logger.warn("Skipping file as it's not a valid CAMT.053 XML: {}", entry.name)
                }
                entry = zipStream.nextEntry
            }
        }
        return documents
    }

    private fun parseCamt053(xml: String): MxCamt05300102? =
        try {
            MxCamt05300102.parse(xml)
        } catch (_: Exception) {
            null
        }

    fun getPreviewsFromDocuments(documents: List<MxCamt05300102>): List<TransactionPreview> {
        val filters = camtFilterRepository.findAll()
        return documents
            .flatMap { doc -> doc.bkToCstmrStmt.stmt }
            .flatMap { stmt ->
                stmt.ntry.map { entry ->
                    cleanUpEntry(entry)
                    val (name, notes) = extractNameAndNotes(entry)
                    val preview =
                        TransactionPreview(
                            id = generateId(entry, stmt),
                            date = entryDateTime(entry, stmt),
                            amount = entry.amt.value,
                            currency = entry.amt.ccy,
                            name = name,
                            iban = extractIban(entry) ?: "",
                            notes = notes,
                            isDebit = entry.cdtDbtInd == com.prowidesoftware.swift.model.mx.dic.CreditDebitCode.DBIT,
                        )
                    applyFilters(preview, filters)
                    preview
                }
            }.sortedByDescending { it.date }
    }

    fun mergeFromDocuments(
        documents: List<MxCamt05300102>,
        excludedIds: List<String> = emptyList(),
    ): ByteArray {
        require(documents.isNotEmpty()) { "No valid CAMT documents provided" }

        val merged = merge(documents, excludedIds)

        // The Prowide library generates XML with a 'camt:' prefix (e.g., <camt:Document>).
        // Many external tools and parsers expect the default namespace instead.
        // We replace the prefixes to ensure maximum compatibility.
        val xmlString =
            merged
                .message()
                .replace("<camt:", "<") // Remove prefix from opening tags
                .replace("</camt:", "</") // Remove prefix from closing tags
                .replace("xmlns:camt=", "xmlns=") // Convert prefixed namespace to default

        return xmlString.toByteArray(Charsets.UTF_8)
    }

    private fun generateId(
        entry: ReportEntry2,
        stmt: AccountStatement2,
    ): String =
        entry.acctSvcrRef ?: run {
            val date = entryDateTime(entry, stmt).toString()
            val amount = entry.amt.value.toString()
            val (name, _) = extractNameAndNotes(entry)
            val iban = extractIban(entry) ?: ""
            // Simple stable ID based on key fields
            val raw = "$date|$amount|$name|$iban"
            Base64
                .getEncoder()
                .encodeToString(raw.toByteArray())
        }

    private fun entryDateTime(
        entry: ReportEntry2,
        stmt: AccountStatement2,
    ): LocalDateTime {
        // Try value date
        entry.valDt?.dtTm?.let { return it.toLocalDateTime() }
        entry.valDt?.dt?.let { return it.atStartOfDay() }

        // Try booking date
        entry.bookgDt?.dtTm?.let { return it.toLocalDateTime() }
        entry.bookgDt?.dt?.let { return it.atStartOfDay() }

        // Fallback to statement date
        return stmtDate(stmt).atStartOfDay()
    }

    fun saveFilter(filter: CamtFilter) {
        camtFilterRepository.save(filter)
    }

    fun deleteFilter(id: UUID) {
        camtFilterRepository.deleteById(id)
    }

    fun getAllFilters(): List<CamtFilter> = camtFilterRepository.findAll()

    internal fun extractNameAndNotes(entry: ReportEntry2): Pair<String, String?> {
        // 1. Try structured fields
        entry.ntryDtls?.forEach { details ->
            details.txDtls?.forEach { tx ->
                tx.rltdPties?.let { parties ->
                    parties.cdtr?.nm?.let { return it to entry.addtlNtryInf }
                    parties.dbtr?.nm?.let { return it to entry.addtlNtryInf }
                }
            }
        }

        val info = entry.addtlNtryInf ?: return "" to null

        // 2. Try BEA/GEA specific parsing
        // [Prefix], [Method]                 [Name]                    [Details]
        // We use a more specific regex that looks for the 'NR:' marker, a date pattern, or a large gap
        // to avoid splitting merchant names that contain double spaces (like "SumUp  *Merchant").
        val cardRegex = Regex("""^(BEA|GEA),\s+(.*?)\s{2,}(.*?)\s{2,}((?:NR:|\d{2}\.\d{2}\.\d{2}/|\s{5,}).*)$""")
        val cardMatch = cardRegex.find(info)
        if (cardMatch != null) {
            val prefix = cardMatch.groupValues[1]
            val method = cardMatch.groupValues[2].trim()
            val merchant = cardMatch.groupValues[3].trim()
            val details = cardMatch.groupValues[4].trim().replace(Regex("\\s{2,}"), " ")
            return merchant to "$prefix, $method, $details"
        }

        // 3. Fallback to /NAME/ tag
        val nameTag =
            Regex("/NAME/([^/]+)(?:/|$)")
                .find(info)
                ?.groupValues
                ?.get(1)
                ?.trim()
        if (nameTag != null) {
            return nameTag to info
        }

        // 4. Final fallback: full string
        return info to null
    }

    internal fun extractIban(entry: ReportEntry2): String? {
        entry.ntryDtls?.forEach { details ->
            details.txDtls?.forEach { tx ->
                tx.rltdPties?.let { parties ->
                    parties.cdtrAcct
                        ?.id
                        ?.iban
                        ?.let { return it }
                    parties.dbtrAcct
                        ?.id
                        ?.iban
                        ?.let { return it }
                }
            }
        }
        return entry.addtlNtryInf?.let { info ->
            Regex("/IBAN/([^/]+)(?:/|$)")
                .find(info)
                ?.groupValues
                ?.get(1)
                ?.trim()
        }
    }

    internal fun applyFilters(
        preview: TransactionPreview,
        filters: List<CamtFilter>,
    ) {
        for (filter in filters) {
            val nameMatches = filter.namePattern?.let { preview.name.contains(it, ignoreCase = true) } ?: false
            val ibanMatches = filter.ibanPattern?.let { preview.iban.contains(it, ignoreCase = true) } ?: false

            val matches =
                if (filter.isStrict) {
                    // If strict: both name and IBAN must match (if patterns are provided)
                    val needsName = filter.namePattern != null
                    val needsIban = filter.ibanPattern != null
                    (if (needsName) nameMatches else true) &&
                        (if (needsIban) ibanMatches else true) &&
                        (needsName || needsIban)
                } else {
                    // If not strict: either name or IBAN matches
                    nameMatches || ibanMatches
                }

            if (matches) {
                preview.isFiltered = true
                preview.filterReason = "Matched filter: ${filter.namePattern ?: filter.ibanPattern}"
                preview.matchedFilterId = filter.id
                return
            }
        }
    }

    private fun merge(
        documents: List<MxCamt05300102>,
        excludedIds: List<String>,
    ): MxCamt05300102 {
        val allStatements =
            documents
                .flatMap { it.bkToCstmrStmt.stmt }
                .sortedBy { stmt -> stmtDate(stmt) }

        val allEntries =
            allStatements.flatMap { stmt ->
                stmt.ntry.filter { entry ->
                    val id = generateId(entry, stmt)
                    !excludedIds.contains(id)
                }
            }
        logger.info(
            "Merging {} entries ({} excluded) from {} statements across {} files",
            allEntries.size,
            excludedIds.size,
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

    private fun cleanUpEntry(entry: ReportEntry2) {
        val (name, notes) = extractNameAndNotes(entry)
        if (name.isNotEmpty() && name != entry.addtlNtryInf) {
            entry.addtlNtryInf = name

            if (notes != null) {
                val details = entry.ntryDtls?.firstOrNull() ?: EntryDetails1().also { entry.addNtryDtls(it) }
                val tx = details.txDtls?.firstOrNull() ?: EntryTransaction2().also { details.addTxDtls(it) }
                val rmtInf = tx.rmtInf ?: RemittanceInformation5().also { tx.rmtInf = it }
                if (rmtInf.ustrd.isEmpty()) {
                    rmtInf.addUstrd(notes)
                }
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
            ?: throw IllegalArgumentException("Missing transaction date")
}
