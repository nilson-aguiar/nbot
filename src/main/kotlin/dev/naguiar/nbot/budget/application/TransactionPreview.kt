package dev.naguiar.nbot.budget.application

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class TransactionPreview(
    val id: String,
    val date: LocalDateTime,
    val amount: BigDecimal,
    val currency: String,
    val name: String,
    val iban: String,
    val notes: String? = null,
    val isDebit: Boolean = true,
    var isFiltered: Boolean = false,
    var filterReason: String? = null,
    var matchedFilterId: UUID? = null,
)
