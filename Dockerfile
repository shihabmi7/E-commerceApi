# Use OpenJDK 17 base image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ecommerce_api-0.0.1-SNAPSHOT.jar /app/app.jar
# COPY wait-for-it.sh wait-for-it.sh
# RUN chmod +x wait-for-it.sh
EXPOSE 8080
# Wait for MySQL and RabbitMQ before starting the app
ENTRYPOINT ["java", "-jar", "app.jar"]
