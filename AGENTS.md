# `nbot` AI Agent Context & Guidelines

Welcome! If you are an AI assistant helping with `<nbot>`, read these guidelines to stay aligned with the project's architecture, security constraints, and user preferences.

## 📌 Project Overview
`nbot` is a Kotlin Spring Boot application that manages torrents via Telegram and provides a real-time web dashboard. While it is designed to use Google Gemini for proactive assistance, **AI features are currently disabled** in favor of direct, deterministic torrent forwarding.

## 🛠 Technology Stack
- **Language**: Kotlin 1.9.25 (JDK 21)
- **Framework**: Spring Boot 3.5.x
- **Web UI**: HTMX, Thymeleaf, Tailwind CSS. We prefer server-side rendering with HTMX for interactivity.
- **Telegram SDK**: `telegrambots-spring-boot-starter`
- **Testing**: JUnit 5, AssertJ (`assertThat`), MockK. **Do not use standard Mockito.**
- **Infrastructure**: Docker, Helm, GitHub Actions.

## 📁 Architecture Pattern
The application follows a Clean Code structure with feature-based packaging.

- **`/presentation/telegram`**: Entry point for Telegram updates. Currently handles logic for magnet links and torrent files directly.
- **`/presentation/web`**: Web controllers serving Thymeleaf templates and HTMX fragments.
- **`/application`**: Core business logic.
  - `/web`: Services for dashboard data and metrics.
- **`/infrastructure`**: 
  - `/config`: Configuration beans (Telegram, qBittorrent, etc.).
  - `/logging`: Custom `SseLogbackAppender` and `SseLogEmitterService` for streaming logs to the web UI.
- **`/tools`**: Planned location for AI-invokable tools (e.g., `/tools/torrent`). Methods use Spring AI's `@Tool` annotation.

## 📍 Current AI Status: DISABLED
Spring AI (Gemini/Ollama) integration is currently **inactive**. 
- The `ChatOrchestrator.kt` and tool-calling logic are present but bypassed.
- The `NbotTelegramObserver` handles updates using standard Kotlin logic.
- **Do not attempt to fix or re-enable AI features** unless explicitly directed by the user.

## 🔐 Security & Guardrails
- **Whitelist**: Only authorized Telegram User IDs (`nbot.security.allowed-users`) can interact with the bot.
- **Secrets**: Credentials (Gemini, Telegram, qBittorrent) are injected via environment variables.

## 🏗 Infrastructure & Deployment
- **Docker**: `Dockerfile` and `docker-compose.yml` are configured for local development and containerization.
- **Kubernetes**: A Helm chart is located in `charts/nbot/`.
- **CI/CD**: `.github/workflows/build.yml` handles automated builds and tests.

## 🧠 Future Development Rules
1. **HTMX/Thymeleaf**: When adding dashboard features, prefer HTMX fragments over full page reloads. Keep JavaScript to a minimum.
2. **Logging**: New features should log meaningful events; they will be automatically streamed to the dashboard if using the standard SLF4J logger.
3. **Clean Boundaries**: Maintain the separation between Telegram presentation, Web presentation, and core Application logic.
4. **Testing**: Always add unit or integration tests for new features using MockK and AssertJ.
