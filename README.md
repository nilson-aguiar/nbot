# nbot

[![Views](https://hits.sh/github.com/nilson-aguiar/nbot.svg?view=today-total&label=Views)](https://hits.sh/github.com/nilson-aguiar/nbot/)

`nbot` is a Kotlin-based Spring Boot application that provides a Telegram/WhatsApp bot interface for managing torrents, a Kubernetes homelab homepage, CAMT.053 XML tools, and a Social Schools media downloader, complemented by an HTMX web dashboard.

## 🚀 Features

- **Telegram & WhatsApp Bot**: Handles magnet links and `.torrent` file uploads for authorized users.
- **qBittorrent Integration**: Automatically forwards torrents to a configured qBittorrent instance.
- **Homelab Homepage**: Discovers all Traefik `IngressRoute` resources in the Kubernetes cluster and presents them at `/` (and `/dashboard`) grouped by namespace (see [Kubernetes RBAC setup](docs/kubernetes-rbac.md)).
- **CAMT XML Tools**: Merge multiple CAMT.053 bank statement XMLs from a ZIP archive and apply custom IBAN/Name filters to exclude transactions.
- **Social Schools Downloader**: Fetch and download photo/video albums from Social Schools via Playwright.
- **Web Dashboard**: Interactive HTMX-based interface with registered bot tools, live execution logs (SSE), CAMT tools, and Social Schools management.
- **Containerized**: Ready for Docker and Kubernetes (Helm).

## 🛠 Tech Stack

- **Backend**: Kotlin 2.x, Java 25, Spring Boot 4.1.x
- **Frontend**: HTMX, Thymeleaf, Tailwind CSS
- **Infrastructure**: Docker, Docker Compose, Helm (Kubernetes)
- **CI/CD**: GitHub Actions

## 💻 Local Development

### Prerequisites
- JDK 25+
- Docker & Docker Compose

### Running Locally
1. Clone the repository.
2. Configure your environment variables in a `.env` file (see `.env.example`).
3. Start the infrastructure (PostgreSQL, qBittorrent) using Docker Compose:
   ```bash
   docker-compose up -d
   ```
4. Run the application:
   ```bash
   ./gradlew bootRun
   ```
5. Access the homepage at `http://localhost:8080/` and the dashboard at `http://localhost:8080/dashboard`.

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
