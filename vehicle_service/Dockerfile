# Vehicle Service Dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Run application
EXPOSE 8082
CMD ["java", "-jar", "target/vehicle_service-1.0.0.jar"]
