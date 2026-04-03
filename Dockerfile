# Stage 1: Build
# Using Maven 3.9 with Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
# Using the stable Temurin Java 21 image
FROM eclipse-temurin:21-jdk
COPY --from=build /target/*.jar app.jar

# Expose your REST and gRPC ports
EXPOSE 8080
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app.jar"]