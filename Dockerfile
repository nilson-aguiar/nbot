# syntax=docker/dockerfile:1
ARG BUILD_STAGE=builder

FROM --platform=$BUILDPLATFORM eclipse-temurin:25.0.5_8-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM --platform=$BUILDPLATFORM scratch AS prebuilt
COPY build/libs/*.jar /app/build/libs/

FROM ${BUILD_STAGE} AS jar-source

FROM mcr.microsoft.com/playwright/java:v1.65.1-noble
COPY --from=eclipse-temurin:25.0.5_8-jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app
COPY --from=jar-source /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
