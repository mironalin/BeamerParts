# BeamerParts Development Commands

## Build and Compilation
```bash
# Clean build all services
mvn clean install

# Clean build with tests
mvn clean install -Dmaven.test.skip=false

# Compile only (faster for development)
mvn clean compile

# Build specific service
cd user-service && mvn clean package
```

## Development Workflow
```bash
# Start development environment
./start-dev.sh

# Start all services
./scripts/dev/start-all-services.sh

# Quick restart specific service
./scripts/dev/quick-restart-service.sh user

# Rebuild and restart services
./scripts/dev/rebuild-restart-services.sh

# Stop all services
./scripts/dev/stop-all.sh
```

## Testing
```bash
# Run all tests
mvn test

# Run tests for specific service
cd user-service && mvn test

# Test API Gateway routes
./scripts/dev/test-api-gateway.sh

# Development test script
./scripts/dev/run-tests.sh
```

## Infrastructure Management
```bash
# Start infrastructure only (PostgreSQL, Redis, RabbitMQ)
docker-compose -f docker-compose.infra.yml up -d

# Full stack with services
docker-compose up -d

# View logs
./scripts/dev/show-all-logs.sh
./scripts/dev/show-service-logs.sh user-service
./scripts/dev/show-infra-logs.sh
```

## Health Checks
```bash
# Check service status
./scripts/dev/check-status.sh

# Individual service health
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Vehicle Service
curl http://localhost:8083/actuator/health  # Product Service
curl http://localhost:8080/actuator/health  # API Gateway
```

## Development Tools
```bash
# Clean build (remove targets)
./scripts/dev/clean-build.sh

# Git operations (standard)
git status
git add .
git commit -m "message"
git push origin main
```