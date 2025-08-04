# Product Service Dockerfile - Multi-stage build
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy parent POM and Maven wrapper from Product Service
COPY pom.xml ./
COPY product_service/mvnw ./
COPY product_service/.mvn/ .mvn

# Make Maven wrapper executable
RUN chmod +x mvnw

# Copy all modules (needed for reactor build)
COPY shared/ shared/
COPY api_gateway/ api_gateway/
COPY user_service/ user_service/
COPY vehicle_service/ vehicle_service/
COPY product_service/ product_service/
COPY order_service/ order_service/

# Build the shared module first, then the Product Service
RUN ./mvnw clean install -pl shared -am -DskipTests
RUN ./mvnw clean package -pl product_service -am -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/product_service/target/product_service-1.0.0.jar app.jar

# Run application
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
