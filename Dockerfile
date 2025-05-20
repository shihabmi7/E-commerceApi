# Use OpenJDK 17 base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the jar file from your local app/target folder to container
# this ecommerce_api-0.0.1-SNAPSHOT.jar creates when
# we run this command -> mvn clear package
# make sure you have this plug in added in pom file > spring-boot-maven-plugin
COPY target/ecommerce_api-0.0.1-SNAPSHOT.jar app.jar

# Expose port (update if your app uses another)
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
