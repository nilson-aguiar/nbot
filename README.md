# nbot

`nbot` is a Kotlin-based Spring Boot application that provides a Telegram bot interface for managing torrents, complemented by a real-time web dashboard.

## 🚀 Features

- **Telegram Bot**: Handles magnet links and `.torrent` file uploads.
- **qBittorrent Integration**: Automatically forwards torrents to a configured qBittorrent instance.
- **Web Dashboard**: Monitor system metrics (CPU, Memory, Uptime) and live execution logs in real-time.
- **Containerized**: Ready for Docker and Kubernetes (Helm).

## 🛠 Tech Stack

- **Backend**: Kotlin 1.9.25, Spring Boot 3.5.x
- **Frontend**: HTMX, Thymeleaf, Tailwind CSS (via CDN)
- **Infrastructure**: Docker, Docker Compose, Helm (Kubernetes)
- **CI/CD**: GitHub Actions

## 📍 Current State: Direct Forwarding

Currently, `nbot` operates as a **deterministic torrent forwarder**.
- When a user sends a magnet link or uploads a `.torrent` file to the Telegram bot, it is directly sent to the qBittorrent Web API.
- The AI-driven reasoning (Spring AI) is **temporarily disabled** while the core integration and infrastructure are being polished.

## 🧠 Roadmap: AI Bot Integration

The next phase of development will re-enable and expand the AI capabilities:
- **Proactive Assistant**: Transition from deterministic rules to a Gemini-powered agent using Spring AI.
- **Function Calling**: The LLM will use `@Tool` annotations to interact with external services (qBittorrent, search engines, etc.).
- **Natural Language Control**: Users will be able to query torrent status, search for content, and manage their download queue using natural language.

## 💻 Local Development

### Prerequisites
- JDK 21+
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
