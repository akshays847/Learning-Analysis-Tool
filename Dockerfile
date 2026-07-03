# --- Build stage: compile the app with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the project and build the jar (skip tests for faster deploys)
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Run stage: small image with just the JRE + the jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/learning-analysis-tool-1.0.0.jar app.jar
# Render provides $PORT; the app reads it via application.properties
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
