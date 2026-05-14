# `nbot` AI Agent Context & Guidelines

Welcome! If you are an AI assistant helping with `<nbot>`, read these guidelines to stay aligned with the project's architecture, security constraints, and user preferences.

## 📌 Project Overview
`nbot` is a Kotlin Spring Boot application that provides two core features:
1. **Media Management**: Forwards torrents and magnet links to qBittorrent via Telegram.
2. **Budget Management**: Integrates with Actual Budget to parse CAMT.053 bank exports, map payees using AI, and sync transactions.

## 🛠 Technology Stack
- **Language**: Kotlin 2.x (Java 25)
- **Framework**: Spring Boot 3.5.x
- **Web UI**: HTMX, Thymeleaf, Tailwind CSS. We use server-side rendering with HTMX for high interactivity with minimal JS.
- **AI**: Spring AI (Gemini) with structured JSON outputs.
- **Telegram SDK**: `telegrambots-spring-boot-starter`
- **Testing**: JUnit 5, AssertJ (`assertThat`), MockK, Testcontainers (PostgreSQL). **Do not use standard Mockito.**
- **Infrastructure**: Docker, Helm, GitHub Actions.

## 📁 Architecture Pattern
The application follows a Clean Code structure with feature-based packaging.

- **`/budget`**: Module for financial integration.
    - `domain`: Entities (`PayeeMapping`, `TransactionDraft`) and Enums.
    - `application`: Core logic including `CamtParserService` (XML), `MappingEngineService` (Regex + AI), and `BudgetAiService`.
    - `infrastructure`: Generated Spring HTTP Interfaces (via OpenAPI Generator) and JPA repositories.
- **`/presentation/telegram`**: Entry point for Telegram updates. Handles both torrent documents and CAMT.053 XML bank statements.
- **`/presentation/web`**: Web controllers. The `DashboardController` includes a dedicated "Budget" view.
- **`/infrastructure`**: Shared infrastructure like configuration, security, and the custom `SseLogbackAppender` for live dashboard logs.

## 📍 AI Status: HYBRID
- **General Chat**: Still largely bypassed or disabled in `ChatOrchestrator.kt`.
- **Feature AI**: **ACTIVE**. The Budget module uses `BudgetAiService` to intelligently map payees and detect internal transfers. It is designed to be resilient and will return `null` if the AI infrastructure is not configured.
- **Structured Output**: We prefer using `.entity(Class)` in Spring AI to get deterministic JSON responses from the LLM.

## 🔐 Security & Guardrails
- **Whitelist**: Only authorized Telegram User IDs (`nbot.security.allowed-users`) can interact with the bot.
- **Environment Secrets**: All credentials (Gemini, Telegram, Actual Budget) are injected via environment variables.

## 🧠 Development Rules
1. **HTMX/Thymeleaf**: Prefer HTMX fragments for dashboard updates. Use the sidebar pattern for tab navigation.
2. **Clean Boundaries**: All budget-related logic MUST stay inside the `dev.naguiar.nbot.budget` package.
3. **Internal Transfers**: When updating mapping logic, respect the `internalAccounts` configuration to prevent transfers from being treated as expenses.
4. **Resiliency**: Services depending on AI should use `ObjectProvider` or null-checks to ensure the application starts even if AI profiles are inactive.
5. **Testing**: New features require unit tests (MockK) and, if they touch the database, integration tests (Testcontainers).
6. **OpenAPI**: The Actual Budget API client is generated from `src/main/resources/api/actual-budget-swagger.json`. Do not edit files in `infrastructure/api/generated` manually. Run `./gradlew openApiGenerate` if the spec changes.
7. **Domain-Persistence Separation**: For entities with complex logic or requiring immutability (like `TransactionDraft`), separate the pure domain model (`data class`) from the JPA entity (`class`). Use repository adapters to manage mapping between these layers.
