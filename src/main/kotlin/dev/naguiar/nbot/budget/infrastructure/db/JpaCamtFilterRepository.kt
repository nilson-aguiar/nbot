package dev.naguiar.nbot.budget.infrastructure.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface JpaCamtFilterRepository : JpaRepository<CamtFilterEntity, UUID>
