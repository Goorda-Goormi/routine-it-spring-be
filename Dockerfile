# Build stage
FROM gradle:7.6-jdk17 AS build
WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY settings.gradle .
COPY build.gradle .

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM openjdk:26-ea-17-jdk-slim
WORKDIR /app

# Copy jar file from build stage
COPY --from=build /app/build/libs/routine-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]