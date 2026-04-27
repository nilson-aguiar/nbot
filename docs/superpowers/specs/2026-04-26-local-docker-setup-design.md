# Design: Local Development Environment with Docker Compose

## 1. Overview
This design provides a `docker-compose.yml` file to run the `nbot` application locally with its dependencies: PostgreSQL and qBittorrent.

## 2. Architecture
The environment consists of three containerized services running on a shared bridge network.

### 2.1 Services

| Service | Image | Purpose | Ports |
|---------|-------|---------|-------|
| `app` | (build .) | Spring Boot Application | 8080:8080 |
| `db` | `postgres:17-alpine` | PostgreSQL Database | 5432:5432 |
| `qbittorrent` | `linuxserver/qbittorrent:latest` | Torrent Management | 8090:8080, 6881:6881 |

### 2.2 Volumes
- `postgres_data`: Persistent storage for the database.
- `qbittorrent_config`: Configuration for qBittorrent.
- `qbittorrent_downloads`: Downloaded files.

## 3. Configuration
- **Environment Variables**: A `.env` file (not committed) will be used for sensitive tokens (`TELEGRAM_BOT_TOKEN`, `GEMINI_API_KEY`, etc.).
- **Health Checks**: The `db` service will include a health check. The `app` will use `depends_on` with `condition: service_healthy`.

## 4. Success Criteria
1. Running `docker compose up` starts all services.
2. The `app` successfully connects to the database.
3. The `app` can communicate with qBittorrent.
4. Data persists across container restarts.
