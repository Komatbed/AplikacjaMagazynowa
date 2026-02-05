# Build Stage
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
# Use gradle directly instead of wrapper
RUN gradle bootJar -x test --no-daemon

# Run Stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
