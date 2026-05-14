# nbot

`nbot` is a Kotlin-based Spring Boot application that provides a Telegram bot interface for managing torrents, complemented by a real-time web dashboard.

## 🚀 Features

- **Telegram Bot**: Handles magnet links, `.torrent` file uploads, and CAMT.053 bank statement processing.
- **qBittorrent Integration**: Automatically forwards torrents to a configured qBittorrent instance.
- **Budget Management**: Parse bank statements, use AI for intelligent payee mapping, and sync transactions to Actual Budget.
- **Web Dashboard**: Monitor system metrics (CPU, Memory, Uptime), live execution logs, and manage pending budget drafts in real-time.
- **Containerized**: Ready for Docker and Kubernetes (Helm).

## 🛠 Tech Stack

- **Backend**: Kotlin 2.x, Java 25, Spring Boot 3.5.x (following Clean Architecture principles)
- **Frontend**: HTMX, Thymeleaf, Tailwind CSS (via CDN)
- **Infrastructure**: Docker, Docker Compose, Helm (Kubernetes)
- **CI/CD**: GitHub Actions

## 📍 Current State: Hybrid Assistant

Currently, `nbot` operates as a **hybrid assistant**.
- **Torrents**: Direct forwarding of magnet links and `.torrent` files to qBittorrent.
- **Budget**: Fully functional bank statement parsing and AI-assisted syncing to Actual Budget.
- **Dashboard**: Interactive HTMX-based interface for metrics, logs, and budget approval.

## 🧠 Roadmap: AI Bot Integration

The next phase of development will re-enable and expand the AI capabilities:
- **Proactive Assistant**: Transition from deterministic rules to a Gemini-powered agent using Spring AI.
- **Function Calling**: The LLM will use `@Tool` annotations to interact with external services (qBittorrent, search engines, etc.).
- **Natural Language Control**: Users will be able to query torrent status, search for content, and manage their download queue using natural language.

## 💻 Local Development

### Prerequisites
- JDK 25+
- Docker & Docker Compose

### Running Locally
1. Clone the repository.
2. Configure your environment variables in a `.env` file (see `.env.example`).
3. Start the infrastructure (e.g., qBittorrent) using Docker Compose:
   ```bash
   docker-compose up -d
   ```
4. Run the application:
   ```bash
   ./gradlew bootRun
   ```
5. Access the dashboard at `http://localhost:8080/dashboard`.

## 🚢 Deployment

### Docker
Build the image:
```bash
docker build -t nbot:latest .
```

### Kubernetes (Helm)
A Helm chart is available in the `charts/nbot/` directory.
```bash
helm install nbot ./charts/nbot -f ./charts/nbot/values.yaml
```

## 🏗 CI/CD
The project uses GitHub Actions for continuous integration. Every push to the `main` branch triggers a build and test workflow.
