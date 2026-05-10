# Dashboard Sync Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the dashboard integration by adding a sync endpoint and property for the default account ID.

**Architecture:** Update configuration properties to include a default account ID, expose a sync endpoint in the `DashboardController`, and ensure the `ActualBudgetService` handles synchronization correctly.

**Tech Stack:** Kotlin, Spring Boot, Thymeleaf (HTMX)

---

### Task 1: Update Configuration

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/budget/infrastructure/config/ActualBudgetProperties.kt`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add `defaultAccountId` to `ActualBudgetProperties`**

```kotlin
@ConfigurationProperties(prefix = "nbot.actual-budget")
data class ActualBudgetProperties(
    val url: String = "http://localhost:5007",
    val apiKey: String = "",
    val syncId: String = "",
    val defaultAccountId: String = "", // Add this line
    val internalAccounts: List<String> = listOf(...)
)
```

- [ ] **Step 2: Update `application.yml`**

```yaml
nbot:
  actual-budget:
    url: ${ACTUAL_BUDGET_URL:http://localhost:5007}
    api-key: ${ACTUAL_BUDGET_API_KEY:}
    sync-id: ${ACTUAL_BUDGET_SYNC_ID:}
    default-account-id: ${ACTUAL_BUDGET_DEFAULT_ACCOUNT_ID:} # Add this line
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/budget/infrastructure/config/ActualBudgetProperties.kt src/main/resources/application.yml
git commit -m "feat: add default account id configuration"
```

### Task 2: Update ActualBudgetService

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/budget/application/ActualBudgetService.kt`

- [ ] **Step 1: Add empty check for `accountId` in `syncApprovedDrafts`**

```kotlin
    fun syncApprovedDrafts(accountId: String) {
        if (accountId.isEmpty()) {
            log.error("Cannot sync approved drafts: default account ID is not configured")
            return
        }
        log.info("Syncing approved drafts to Actual Budget account: {}", accountId)
        // ... rest of the method
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/budget/application/ActualBudgetService.kt
git commit -m "feat: handle empty accountId in ActualBudgetService"
```

### Task 3: Update DashboardController

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/presentation/web/DashboardController.kt`

- [ ] **Step 1: Inject dependencies and add sync endpoint**

```kotlin
@Controller
class DashboardController(
    private val dataService: DashboardDataService,
    private val logEmitterService: SseLogEmitterService,
    private val budgetImportService: BudgetImportService,
    private val transactionDraftRepository: TransactionDraftRepository,
    private val actualBudgetService: ActualBudgetService, // Inject this
    private val properties: ActualBudgetProperties // Inject this
) {
    // ... existing methods

    @PostMapping("/dashboard/budget/sync")
    fun syncBudget(model: Model): String {
        actualBudgetService.syncApprovedDrafts(properties.defaultAccountId)
        return budgetFragment(model)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/presentation/web/DashboardController.kt
git commit -m "feat: add budget sync endpoint to dashboard"
```

### Task 4: Verification

- [ ] **Step 1: Compile the project**

Run: `./gradlew classes`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Check for compilation errors**
Check if any errors are reported during build.
