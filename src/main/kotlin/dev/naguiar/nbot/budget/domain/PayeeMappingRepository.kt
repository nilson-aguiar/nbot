package dev.naguiar.nbot.budget.domain

import java.util.*

interface PayeeMappingRepository {
    fun findAll(): List<PayeeMapping>
    fun findById(id: UUID): PayeeMapping?
    fun save(payeeMapping: PayeeMapping): PayeeMapping
    fun deleteById(id: UUID)
}
