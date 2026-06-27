package dev.naguiar.nbot.budget.infrastructure.db

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface JpaCamtFilterRepository : JpaRepository<CamtFilterEntity, UUID>
