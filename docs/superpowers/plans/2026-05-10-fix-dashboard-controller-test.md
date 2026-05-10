# Fix DashboardControllerTest Compilation and Add Sync Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix compilation errors in `DashboardControllerTest.kt` by providing missing mocks and add a test case for the new `syncBudget` endpoint.

**Architecture:** Use MockK to mock the new dependencies (`ActualBudgetService` and `ActualBudgetProperties`) and inject them into `DashboardController`.

**Tech Stack:** Kotlin, Spring Boot Test, MockK, MockMvc.

---

### Task 1: Fix Compilation and Add Sync Test

**Files:**
- Modify: `src/test/kotlin/dev/naguiar/nbot/presentation/web/DashboardControllerTest.kt`

- [ ] **Step 1: Add missing mocks and update controller instantiation**

Update `DashboardControllerTest.kt` to include `actualBudgetService` and `properties` mocks.

```kotlin
    private val dataService = mockk<DashboardDataService>()
    private val logEmitterService = mockk<SseLogEmitterService>(relaxed = true)
    private val budgetImportService = mockk<BudgetImportService>()
    private val transactionDraftRepository = mockk<TransactionDraftRepository>()
    private val actualBudgetService = mockk<ActualBudgetService>()
    private val properties = ActualBudgetProperties(defaultAccountId = "test-account")
    private val controller = DashboardController(
        dataService, 
        logEmitterService, 
        budgetImportService, 
        transactionDraftRepository, 
        actualBudgetService, 
        properties
    )
```

- [ ] **Step 2: Add test case for syncBudget endpoint**

Add the following test case to `DashboardControllerTest.kt`:

```kotlin
    @Test
    fun `should sync budget and return budget fragment`() {
        every { actualBudgetService.syncApprovedDrafts("test-account") } returns Unit
        every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()

        mockMvc
            .perform(post("/dashboard/budget/sync"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/budget :: budget"))
            .andExpect(model().attributeExists("drafts"))
    }
```

- [ ] **Step 3: Run tests and verify they pass**

Run: `./gradlew test`
Expected: All tests pass, especially `DashboardControllerTest` and budget-related tests.

- [ ] **Step 4: Commit changes**

```bash
git add src/test/kotlin/dev/naguiar/nbot/presentation/web/DashboardControllerTest.kt
git commit -m "test: fix DashboardControllerTest compilation and add syncBudget test"
```
