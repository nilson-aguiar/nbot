package dev.naguiar.nbot.budget.infrastructure.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaPayeeMappingRepository : JpaRepository<PayeeMappingEntity, UUID>
