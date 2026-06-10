package dev.naguiar.nbot.budget.domain

import java.util.UUID

interface CamtFilterRepository {
    fun findAll(): List<CamtFilter>

    fun save(filter: CamtFilter): CamtFilter

    fun deleteById(id: UUID)
}
