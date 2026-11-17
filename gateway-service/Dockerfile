# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Estagio 1: build do jar com Maven (Java 21 — alinhado ao gateway do professor).
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package \
    && cp target/*.jar /build/app.jar

# ---------------------------------------------------------------------------
# Estagio 2: imagem de execucao (apenas JRE).
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S campusconnect && adduser -S -G campusconnect campusconnect
USER campusconnect

COPY --from=build /build/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
