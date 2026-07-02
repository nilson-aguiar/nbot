FROM eclipse-temurin:25.0.3_9-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM mcr.microsoft.com/playwright/java:v1.61.0-noble
COPY --from=eclipse-temurin:25.0.3_9-jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
