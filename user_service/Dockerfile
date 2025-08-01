# User Service Dockerfile - Multi-stage build
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy parent POM and Maven wrapper from User Service
COPY pom.xml ./
COPY user_service/mvnw ./
COPY user_service/.mvn/ .mvn

# Make Maven wrapper executable
RUN chmod +x mvnw

# Copy all modules (needed for reactor build)
COPY shared/ shared/
COPY api_gateway/ api_gateway/
COPY user_service/ user_service/
COPY vehicle_service/ vehicle_service/
COPY product_service/ product_service/
COPY order_service/ order_service/

# Build the shared module first, then the User Service
RUN ./mvnw clean install -pl shared -am -DskipTests
RUN ./mvnw clean package -pl user_service -am -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/user_service/target/user_service-1.0.0.jar app.jar

# Run application
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
