# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies (this layer is cached)
RUN mvn dependency:go-offline -B
COPY src ./src
# Build the application, skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Render sets the PORT environment variable; default to 8080
ENV PORT=8080
EXPOSE ${PORT}
# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
