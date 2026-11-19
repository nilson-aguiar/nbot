# `nbot` AI Agent Context & Guidelines

Welcome! If you are an AI assistant helping with `nbot`, read these guidelines to stay aligned with the project's architecture, security constraints, and user preferences.

## 📌 Project Overview
`nbot` is a Kotlin Spring Boot application that provides four core features:
1. **Media Management**: Forwards torrents and magnet links to qBittorrent via Telegram and WhatsApp.
2. **Homelab Homepage**: Discovers Kubernetes Traefik `IngressRoute` resources and displays them at `/` (and as the default `/dashboard` view).
3. **CAMT XML Tools**: Parses, filters, and merges CAMT.053 bank statement XML exports in `/dashboard/tools`.
4. **Social Schools**: Downloads and manages photo/video albums from Social Schools in `/dashboard/socialschools`.

## 🛠 Technology Stack
- **Language**: Kotlin 2.x (Java 25)
- **Framework**: Spring Boot 4.1.x
- **Web UI**: HTMX, Thymeleaf, Vanilla CSS / Tailwind CSS. We use server-side rendering with HTMX for high interactivity with minimal JS.
- **Telegram SDK**: `telegrambots-spring-boot-starter`
- **Kubernetes Client**: Fabric8 Kubernetes Client (`io.fabric8:kubernetes-client`)
- **Browser Automation**: Playwright (`com.microsoft.playwright:playwright`) for Social Schools
- **Testing**: JUnit 5, AssertJ (`assertThat`), MockK, Testcontainers (PostgreSQL). **Do not use standard Mockito.**
- **Infrastructure**: Docker, Helm, GitHub Actions.

## 📁 Architecture Pattern
The application follows a Clean Code structure with feature-based packaging.

- **`/budget`**: Module for CAMT.053 XML merging and filtering (used by `ToolsController`).
    - `domain`: Immutable domain models (`CamtFilter`) and repository interfaces (`CamtFilterRepository`).
    - `application`: Core logic including `CamtMergerService` and `TransactionPreview`.
    - `infrastructure`: Persistence entities (`CamtFilterEntity`) and repository adapters (`CamtFilterRepositoryAdapter`, `JpaCamtFilterRepository`).
- **`/tools`**: Feature tools.
    - `BotTool`: Annotation used to register tools shown in the dashboard sidebar.
    - `torrent`: `TorrentTools`, `QBittorrentClient`, `QBittorrentApi`, `QBittorrentProperties`.
    - `socialschools`: `SocialSchoolsService`, `SocialSchoolsClient`, `SocialSchoolsAlbum`, and persistence adapters.
- **`/presentation/telegram`**: Entry point for Telegram updates (`NbotTelegramObserver`).
- **`/presentation/whatsapp`**: Entry point for WhatsApp webhooks (`WhatsAppWebhookController`, `WhatsAppClient`).
- **`/presentation/web`**: Web controllers. Standardized with class-level `@RequestMapping` (except root `IndexController`).
    - `IndexController` (`/`): Homelab homepage and `/homepage/refresh`.
    - `DashboardController` (`/dashboard`): Dashboard shell (defaulting to homelab services), live logs (`/dashboard/torrents`), and SSE stream (`/dashboard/logs/stream`).
    - `ToolsController` (`/dashboard/tools`): CAMT merging and filtering tools.
    - `SocialSchoolsController` (`/dashboard/socialschools`): Social Schools album management and downloads.
- **`/infrastructure`**: Shared infrastructure like configuration, properties, and `SseLogbackAppender` / `SseLogEmitterService` for live dashboard logs.

## 🔐 Security & Guardrails
- **Whitelist**: Only authorized Telegram User IDs (`nbot.telegram.allowed-users`) and WhatsApp numbers (`nbot.whatsapp.allowed-numbers`) can interact with the bot.
- **Environment Secrets**: All credentials (Telegram, WhatsApp, qBittorrent, Social Schools) are injected via environment variables.

## 🧠 Development Rules
1. **HTMX/Thymeleaf**: Prefer HTMX fragments for dashboard updates. Use the sidebar pattern for tab navigation.
2. **Controller Standard**: Use class-level `@RequestMapping` for all web controllers to maintain a clean hierarchical routing structure.
3. **Date Priority**: In CAMT parsing, ALWAYS prioritize `ValDt` (Value Date) over `BookgDt` (Booking Date). Booking dates are often batched by banks, while value dates represent the actual transaction occurrence.
4. **ZIP Resiliency**: When importing ZIP archives, use a one-pass parsing approach. Attempt to parse each XML and skip files that are invalid or metadata (e.g., macOS `__MACOSX` files) instead of allowing the batch to fail.
5. **Clean Boundaries**: Keep CAMT merging/filtering logic inside `dev.naguiar.nbot.budget`.
6. **Testing**: New features require unit tests (MockK) and, if they touch the database, integration tests (Testcontainers).
7. **Domain-Persistence Separation**: For entities with complex logic or requiring immutability (like `CamtFilter` and `SocialSchoolsAlbum`), separate the pure domain model (`data class`) from the JPA entity (`class`). Use repository adapters to manage mapping between these layers.
8. **Check `docs/learnings/`**: This directory accumulates non-obvious gotchas discovered while working on the code (e.g., framework quirks, parser surprises). Skim relevant files before debugging in unfamiliar areas.
9. **Import Preference**: Never use fully qualified class names anywhere in the code (methods, variables, types, etc.); always use imports to keep the code clean and idiomatic. To make code context-rich and avoid ambiguity, using **import aliases** is encouraged (e.g., `import org.telegram...File as TelegramFile`), especially when dealing with classes that have similar names across different packages.
