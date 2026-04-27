# Local Development Docker Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a Docker Compose environment to run the nbot application, PostgreSQL, and qBittorrent locally.

**Architecture:** A multi-container setup using Docker Compose. The application container will depend on a healthy PostgreSQL container. Volumes are used for persistence and mapped to `.docker_data/` (ignored by git).

**Tech Stack:** Docker, Docker Compose, PostgreSQL 17, qBittorrent, Spring Boot.

---

### Task 1: Project Environment Preparation

**Files:**
- Create: `.env.example`
- Modify: `.gitignore` (already done, but verify)

- [ ] **Step 1: Create .env.example template**
Create a template file with necessary environment variables for the user to fill in.

```bash
cat <<EOF > .env.example
# Telegram
TELEGRAM_BOT_TOKEN=your_token_here
ALLOWED_USERS=user1,user2

# Google Gemini
GEMINI_API_KEY=your_api_key_here

# Database
POSTGRES_USER=nbot
POSTGRES_PASSWORD=nbot_password
POSTGRES_DB=nbot

# qBittorrent (defaults for linuxserver/qbittorrent)
QBITTORRENT_USERNAME=admin
QBITTORRENT_PASSWORD=adminadmin
EOF
```

- [ ] **Step 2: Ensure .docker_data is ignored**
Verify `.gitignore` contains `.docker_data/`.

Run: `grep ".docker_data/" .gitignore`
Expected: `.docker_data/`

- [ ] **Step 3: Create volume directories**
Create the local directories where data will be persisted.

Run: `mkdir -p .docker_data/postgres .docker_data/qbittorrent_config .docker_data/qbittorrent_downloads`

- [ ] **Step 4: Commit**
```bash
git add .env.example .gitignore
git commit -m "chore: setup environment for local docker"
```

### Task 2: Docker Compose Configuration

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Create docker-compose.yml**
Define services for `app`, `db`, and `qbittorrent`.

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/\${POSTGRES_DB:-nbot}
      - SPRING_DATASOURCE_USERNAME=\${POSTGRES_USER:-nbot}
      - SPRING_DATASOURCE_PASSWORD=\${POSTGRES_PASSWORD:-nbot_password}
      - TELEGRAM_BOT_TOKEN=\${TELEGRAM_BOT_TOKEN}
      - ALLOWED_USERS=\${ALLOWED_USERS}
      - GEMINI_API_KEY=\${GEMINI_API_KEY}
      - QBITTORRENT_URL=http://qbittorrent:8080
      - QBITTORRENT_USERNAME=\${QBITTORRENT_USERNAME:-admin}
      - QBITTORRENT_PASSWORD=\${QBITTORRENT_PASSWORD:-adminadmin}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:17-alpine
    environment:
      - POSTGRES_USER=\${POSTGRES_USER:-nbot}
      - POSTGRES_PASSWORD=\${POSTGRES_PASSWORD:-nbot_password}
      - POSTGRES_DB=\${POSTGRES_DB:-nbot}
    volumes:
      - ./.docker_data/postgres:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \${POSTGRES_USER:-nbot} -d \${POSTGRES_DB:-nbot}"]
      interval: 5s
      timeout: 5s
      retries: 5

  qbittorrent:
    image: linuxserver/qbittorrent:latest
    container_name: qbittorrent
    environment:
      - PUID=1000
      - PGID=1000
      - TZ=Etc/UTC
      - WEBUI_PORT=8080
    volumes:
      - ./.docker_data/qbittorrent_config:/config
      - ./.docker_data/qbittorrent_downloads:/downloads
    ports:
      - "8090:8080"
      - "6881:6881"
      - "6881:6881/udp"
    restart: unless-stopped
```

- [ ] **Step 2: Commit**
```bash
git add docker-compose.yml
git commit -m "feat: add docker-compose.yml for local development"
```

### Task 3: Application Configuration for Docker Profile

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add docker profile to application.yml**
Configure Spring to use PostgreSQL when the `docker` profile is active.

```yaml
# Add to src/main/resources/application.yml

---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/nbot}
    username: ${SPRING_DATASOURCE_USERNAME:nbot}
    password: ${SPRING_DATASOURCE_PASSWORD:nbot_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

- [ ] **Step 2: Verify application builds**
Run: `./gradlew clean build -x test`

- [ ] **Step 3: Commit**
```bash
git add src/main/resources/application.yml
git commit -m "config: add docker profile for postgresql"
```

### Task 4: Verification

- [ ] **Step 1: Verify docker-compose config**
Run: `docker compose config`
Expected: Valid YAML output.

- [ ] **Step 2: (Manual) Verify full setup**
Instruct user to copy `.env.example` to `.env`, fill it, and run `docker compose up`.
