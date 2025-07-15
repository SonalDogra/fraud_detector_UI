# Use a small JDK image
FROM openjdk:17-alpine

# Set working directory (optional)
WORKDIR /app

# Copy the new JAR into the container
COPY target/fraud_detector-0.0.1-SNAPSHOT.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
