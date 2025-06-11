# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Stage 2: Run
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/ecommerce_api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
#

# # Use OpenJDK 17 base image
# FROM openjdk:17-jdk-slim
# WORKDIR /app
# COPY target/ecommerce_api-0.0.1-SNAPSHOT.jar /app/app.jar
# # COPY wait-for-it.sh wait-for-it.sh
# # RUN chmod +x wait-for-it.sh
# EXPOSE 8080
# # Wait for MySQL and RabbitMQ before starting the app
# ENTRYPOINT ["java", "-jar", "app.jar"]
