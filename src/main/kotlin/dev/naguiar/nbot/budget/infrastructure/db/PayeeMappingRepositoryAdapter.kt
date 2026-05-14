package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.PayeeMapping
import dev.naguiar.nbot.budget.domain.PayeeMappingRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class PayeeMappingRepositoryAdapter(
    private val jpaRepository: JpaPayeeMappingRepository
) : PayeeMappingRepository {

    override fun findAll(): List<PayeeMapping> =
        jpaRepository.findAll().map { it.toDomain() }

    override fun findById(id: UUID): PayeeMapping? =
        jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun save(payeeMapping: PayeeMapping): PayeeMapping =
        jpaRepository.save(PayeeMappingEntity.fromDomain(payeeMapping)).toDomain()

    override fun deleteById(id: UUID) =
        jpaRepository.deleteById(id)
}
