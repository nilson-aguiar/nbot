# `nbot` AI Agent Context & Guidelines

Welcome! If you are an AI assistant helping with `<nbot>`, read these guidelines to stay aligned with the project's architecture, security constraints, and user preferences.

## 📌 Project Overview
`nbot` is a Kotlin Spring Boot Telegram Chatbot designed to run in a containerized environment (Kubernetes/Docker). It acts as a proactive assistant using Google Gemini's function calling (or Ollama locally) to perform actions via external APIs and services based on user input (text or files).

## 🛠 Technology Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.x
- **AI Library**: Spring AI (Google GenAI & Ollama starters). To configure Tools/functions, it leverages the `@Tool` annotation.
- **Telegram SDK**: `telegrambots-spring-boot-starter`
- **Testing**: JUnit 5, AssertJ (`assertThat`), MockK (`@MockK`, `@InjectMockKs`, `every { ... } returns`). Do not use standard Mockito.
- **Build Tool**: Gradle (Kotlin DSL)

## 📁 Architecture Pattern
The application is a standalone microservice following a Clean Code structure mixed with feature-based packaging for its tools.

- **`/presentation/telegram`**: The entry point. Handles Telegram `Update` objects (via long-polling). Contains the `TelegramObserver`.
- **`/application`**: The core business logic layer. Contains `ChatOrchestrator.kt`, orchestrating the flow between Telegram, Spring AI (Gemini/Ollama), and tool execution.
- **`/tools/<feature>` (Tool Registry)**: This is where we define agent capabilities! Features are grouped into cohesive packages. For example, `/tools/torrent` contains:
  - `<Feature>Client.kt`: Dedicated rest client (e.g., using `RestClient`) for interacting with target services.
  - `<Feature>Properties.kt`: The specific configurations.
  - `<Feature>Tools.kt`: Spring beans exposing methods annotated with Spring AI's `@Tool` that the LLM can invoke.

## 🔗 Existing Skills: Torrent Uploader
The bot identifies intent to manage torrents from user messages (magnet links) or uploaded `.torrent` files.
1. **Detection**: `NbotTelegramObserver` identifies a `.torrent` document or magnet link.
2. **Contextualization**: The bot downloads relevant file contents directly from Telegram securely to local storage.
3. **Reasoning**: Gemini/Ollama is prompted with the context and identifies that the `addTorrent` tool should be used.
4. **Execution**: The `addTorrentFile` tool accepts the input and invokes the qBittorrent Web API (`/api/v2/torrents/add`) over `RestClient`. API credentials are provided via configuration (environment variables).
5. **Feedback**: The bot sends a success/failure confirmation back to the user via Telegram.

## 🔐 Security & Guardrails
- **Authentication**: A whitelist of authorized Telegram User IDs (`nbot.security.allowed-users`) ensures only the owner can trigger bot chats and skills. The `NbotTelegramObserver` instantly drops updates from unlisted IDs.
- **Secret Management**: `GEMINI_API_KEY`, `TELEGRAM_BOT_TOKEN`, and service credentials (e.g. qBittorrent) are injected securely via environment variables or secret stores into `application.yml`.
- **Skill Scoping**: The bot only has access to services explicitly defined in its Tool Registry via `.defaultFunctions()` mappings.

## 🚀 Important Commands
- **Compile / Syntax Check**: `./gradlew classes -x test`
- **Run Unit Tests**: `./gradlew test` (Always write AssertJ and MockK unit tests for new features!)
- **Run the Bot**: `./gradlew bootRun`

## 🧠 Future Development Rules
1. **Building new capabilities**: When adding a new capability, create a folder like `/tools/<new_skill>`. Add the specific Rest client, configuration properties, and a `<NewSkill>Tools.kt` class. Annotate public tool methods with `@Tool` and provide an extensive, detailed string `description=` in the annotation.
2. **Spring Context wiring**: Be sure that `ChatOrchestrator` includes the new tool by adding `.defaultTools("<newToolMethodName>")` to its `ChatClient.Builder`.
3. **Configurations**: Maintain `src/main/resources/application.yml` using the multi-document YAML (the `---` separator) so that the `gemini` and `ollama` profiles can dynamically switch functionalities without conflicting.
