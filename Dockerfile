# Multi-stage build for Spring Boot with Maven
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy only Maven wrapper and pom.xml first for better layer caching
COPY .mvn .mvn
COPY mvnw mvnw.cmd pom.xml ./

# Download dependencies (this layer caches as long as pom.xml doesn't change)
RUN ./mvnw dependency:resolve dependency:resolve-plugins

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD java -cp /app/app.jar org.springframework.boot.loader.JarLauncher > /dev/null 2>&1 || exit 1

# Copy only the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
