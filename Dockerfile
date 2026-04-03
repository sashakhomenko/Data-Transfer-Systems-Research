# Stage 1: Build
FROM maven:3.9.6-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM openjdk:21-jdk-slim
COPY --from=build /target/*.jar app.jar

# Expose both ports
EXPOSE 8080
EXPOSE 9090

# We pass the REST port via Railway's PORT env var
# and hardcode 9090 for gRPC for the TCP proxy
ENTRYPOINT ["java", "-jar", "/app.jar"]