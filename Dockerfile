FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set the working directory inside the build container
WORKDIR /app

# Copy pom.xml first — Docker caches this layer separately
# so if only your code changes, it won't re-download all dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy all source code and build the JAR file
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app using a smaller image (no Maven needed at runtime)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Tell Railway which port your app listens on
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]