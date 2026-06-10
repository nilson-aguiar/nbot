package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.CamtFilter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "camt_filter")
class CamtFilterEntity(
    @Id
    var id: UUID,
    @Column(columnDefinition = "TEXT")
    val namePattern: String?,
    @Column(columnDefinition = "TEXT")
    var ibanPattern: String?,
    var isStrict: Boolean = false,
) {
    fun toDomain(): CamtFilter =
        CamtFilter(
            id = id,
            namePattern = namePattern,
            ibanPattern = ibanPattern,
            isStrict = isStrict,
        )

    companion object {
        fun fromDomain(domain: CamtFilter): CamtFilterEntity =
            CamtFilterEntity(
                id = domain.id,
                namePattern = domain.namePattern,
                ibanPattern = domain.ibanPattern,
                isStrict = domain.isStrict,
            )
    }
}

fun CamtFilter.toEntity(): CamtFilterEntity = CamtFilterEntity.fromDomain(this)
