# API Gateway Dockerfile - Multi-stage build
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy parent POM and Maven wrapper from API Gateway
COPY pom.xml ./
COPY api_gateway/mvnw ./
COPY api_gateway/.mvn/ .mvn

# Make Maven wrapper executable
RUN chmod +x mvnw

# Copy all modules (needed for multi-module build)
COPY shared/ shared/
COPY api_gateway/ api_gateway/
COPY user_service/ user_service/
COPY vehicle_service/ vehicle_service/
COPY product_service/ product_service/
COPY order_service/ order_service/

# Build entire project (Spring Boot plugin will create fat JARs automatically)
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/api_gateway/target/api_gateway-1.0.0.jar app.jar

# Run application
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
