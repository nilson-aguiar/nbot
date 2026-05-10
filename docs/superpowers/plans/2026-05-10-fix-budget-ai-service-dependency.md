# Fix BudgetAiService Optional Dependency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `ChatClient.Builder` optional in `BudgetAiService` to allow the application context to load even when AI profiles are inactive.

**Architecture:** Use `@Autowired(required = false)` for `ChatClient.Builder` in the constructor. If null, `chatClient` will be null, and `suggestMapping` will return null.

**Tech Stack:** Kotlin, Spring Boot, Spring AI

---

### Task 1: Modify BudgetAiService.kt

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/budget/application/BudgetAiService.kt`

- [ ] **Step 1: Update constructor and property initialization**

Modify the constructor to make `chatClientBuilder` optional and add `ActualBudgetProperties`.
Initialize `chatClient` using `chatClientBuilder?.let { ... }`.

```kotlin
@Service
class BudgetAiService(
    @Autowired(required = false) private val chatClientBuilder: ChatClient.Builder?,
    private val properties: ActualBudgetProperties
) {
    private val chatClient = chatClientBuilder?.let { builder ->
        builder.defaultSystem("""
            You are a financial assistant helping to map bank transactions to budget payees.
            Your task is to analyze a bank transaction and suggest the most likely payee from a provided list.
            If the transaction appears to be a transfer between internal accounts, identify it as such.
            
            Return your response in structured JSON format with the following fields:
            - payeeId: The ID of the suggested payee from the list, or null if no match found.
            - payeeName: The name of the suggested payee from the list, or null.
            - isTransfer: Boolean indicating if this is an internal transfer between the user's accounts.
            - targetAccountId: The ID of the target internal account if it's a transfer, or null.
            - confidence: A float between 0.0 and 1.0 representing your confidence in this mapping.
            - reasoning: A brief explanation of why you chose this mapping.
        """.trimIndent())
        .build()
    }
    // ...
}
```

- [ ] **Step 2: Update suggestMapping to check for null chatClient**

```kotlin
    fun suggestMapping(
        bankPayeeName: String,
        bankDescription: String,
        knownPayees: List<ActualPayee>,
        internalAccounts: List<InternalAccount>
    ): AiMappingResponse? {
        if (chatClient == null) return null
        
        val promptText = """
            Analyze the following bank transaction:
            Bank Payee Name: ${'$'}bankPayeeName
            Bank Description: ${'$'}bankDescription
            
            Known Payees:
            ${'$'}{knownPayees.joinToString("\n") { "- ${'$'}{it.name} (ID: ${'$'}{it.id})" }}
            
            Internal Accounts:
            ${'$'}{internalAccounts.joinToString("\n") { "- ${'$'}{it.name} (ID: ${'$'}{it.id})" }}
            
            Find the best match or identify if it's an internal transfer.
        """.trimIndent()

        return chatClient.prompt()
            .user(promptText)
            .call()
            .entity(AiMappingResponse::class.java)
    }
```

### Task 2: Verify Fix

- [ ] **Step 1: Run NbotApplicationTests**

Run: `./gradlew test --tests dev.naguiar.nbot.NbotApplicationTests`
Expected: PASS

- [ ] **Step 2: Run all tests to ensure no regressions**

Run: `./gradlew test`
Expected: PASS
