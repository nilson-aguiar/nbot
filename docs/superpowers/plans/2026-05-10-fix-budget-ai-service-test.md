# Fix BudgetAiServiceTest Compilation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the compilation error in `BudgetAiServiceTest.kt` by correctly mocking `ObjectProvider<ChatClient.Builder>`.

**Architecture:** The `BudgetAiService` now expects an `ObjectProvider` to handle optional beans. The test must be updated to provide a mocked `ObjectProvider` that returns the mocked `ChatClient.Builder`.

**Tech Stack:** Kotlin, JUnit 5, MockK, Spring Boot (ObjectProvider)

---

### Task 1: Update BudgetAiServiceTest.kt

**Files:**
- Modify: `src/test/kotlin/dev/naguiar/nbot/budget/application/BudgetAiServiceTest.kt`

- [ ] **Step 1: Update the mocks and service instantiation**

```kotlin
class BudgetAiServiceTest {

    private val chatClientBuilder: ChatClient.Builder = mockk()
    private val chatClientBuilderProvider: ObjectProvider<ChatClient.Builder> = mockk() // NEW
    private val properties = ActualBudgetProperties()
    private val chatClient: ChatClient = mockk()
    private val promptSpec: ChatClient.ChatClientRequestSpec = mockk()
    private val callResponseSpec: ChatClient.CallResponseSpec = mockk()

    @Test
    fun `should suggest mapping correctly`() {
        // Given
        val expectedResponse = BudgetAiService.AiMappingResponse(
            // ...
        )

        every { chatClientBuilderProvider.getIfAvailable() } returns chatClientBuilder // OR ifAvailable property
        // actually looking at the code: chatClientBuilderProvider.ifAvailable
        every { chatClientBuilderProvider.ifAvailable } returns chatClientBuilder

        every { chatClientBuilder.defaultSystem(any<String>()) } returns chatClientBuilder
        every { chatClientBuilder.build() } returns chatClient
        
        val service = BudgetAiService(chatClientBuilderProvider, properties) // Updated constructor call
        // ...
```

- [ ] **Step 2: Verify compilation and tests**

Run: `./gradlew test --tests dev.naguiar.nbot.budget.application.BudgetAiServiceTest`
Expected: PASS

- [ ] **Step 3: Verify NbotApplicationTests**

Run: `./gradlew test --tests dev.naguiar.nbot.NbotApplicationTests`
Expected: PASS
