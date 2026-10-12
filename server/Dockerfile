# syntax=docker/dockerfile:1

# Build stage: compile and package the fat jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon clean buildFatJar

# Runtime stage: slim JRE, only the fat jar.
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*-all.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
