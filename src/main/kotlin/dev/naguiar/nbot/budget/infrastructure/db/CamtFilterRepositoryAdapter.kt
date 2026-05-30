package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.CamtFilter
import dev.naguiar.nbot.budget.domain.CamtFilterRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class CamtFilterRepositoryAdapter(
    private val jpaRepository: JpaCamtFilterRepository,
) : CamtFilterRepository {
    override fun findAll(): List<CamtFilter> = jpaRepository.findAll().map { it.toDomain() }

    override fun save(filter: CamtFilter): CamtFilter =
        jpaRepository.save(CamtFilterEntity.fromDomain(filter)).toDomain()

    override fun deleteById(id: UUID) = jpaRepository.deleteById(id)
}
