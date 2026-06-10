package dev.naguiar.nbot.budget.infrastructure.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaCamtFilterRepository : JpaRepository<CamtFilterEntity, UUID>
