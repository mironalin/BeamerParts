# BeamerParts Task Completion Checklist

## When Completing a Development Task

### 1. Code Quality
- [ ] Code follows package naming conventions (`live.alinmiron.beamerparts.*`)
- [ ] Proper Spring Boot annotations used
- [ ] Business logic in service layer, not controllers
- [ ] DTOs used for API boundaries
- [ ] Exception handling implemented

### 2. Testing
```bash
# Run comprehensive tests
mvn clean test

# Test specific service if changes are isolated
cd {service-name} && mvn test

# Test API Gateway integration
./scripts/dev/test-api-gateway.sh
```

### 3. Build Verification
```bash
# Ensure clean build
mvn clean install

# Verify no compilation errors
mvn clean compile
```

### 4. Service Integration Testing
```bash
# Start services and test integration
./scripts/dev/start-all-services.sh
./scripts/dev/check-status.sh

# Test health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### 5. Database Migrations (if applicable)
- [ ] Flyway migration files properly named and versioned
- [ ] Database changes tested locally
- [ ] Migration rollback plan considered

### 6. Documentation Updates
- [ ] README.md updated if architecture changes
- [ ] API documentation updated for new endpoints
- [ ] Comments added for complex business logic

### 7. Git Workflow
```bash
# Standard git workflow
git add .
git commit -m "feat: descriptive commit message"
git push origin main
```

### 8. Performance Considerations
- [ ] Database queries optimized
- [ ] Caching strategy considered (Redis)
- [ ] Service-to-service communication patterns followed

## Pre-Production Checklist
- [ ] All tests passing
- [ ] Security review completed (especially JWT handling)
- [ ] Configuration externalized (no hardcoded values)
- [ ] Logging levels appropriate
- [ ] Health checks responding correctly
- [ ] Database migration strategy validated