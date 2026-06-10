package dev.naguiar.nbot.budget.domain

import java.util.UUID

data class CamtFilter(
    val id: UUID = UUID.randomUUID(),
    val namePattern: String?,
    val ibanPattern: String?,
    val isStrict: Boolean = false,
)
