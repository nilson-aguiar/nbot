package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.PayeeMapping
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PayeeMappingRepository : JpaRepository<PayeeMapping, UUID>
