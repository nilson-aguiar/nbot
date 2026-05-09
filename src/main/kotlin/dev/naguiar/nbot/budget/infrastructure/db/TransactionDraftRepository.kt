package dev.naguiar.nbot.budget.infrastructure.db

import dev.naguiar.nbot.budget.domain.TransactionDraft
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionDraftRepository : JpaRepository<TransactionDraft, UUID>
