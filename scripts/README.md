# BeamerParts Development Scripts

This folder contains all development, build, and deployment scripts for the BeamerParts microservices project.

## 🚀 Quick Start

The easiest way to get started is to run the interactive development manager:

```bash
./scripts/dev-start.sh
```

This will present you with a menu to choose what you want to do.

## 📁 Folder Structure

```
scripts/
├── dev-start.sh           # Interactive development menu (START HERE)
├── docker/                # Docker infrastructure scripts
│   ├── start-infra.sh     # Start PostgreSQL, Redis, RabbitMQ
│   ├── stop-infra.sh      # Stop infrastructure
│   └── status-infra.sh    # Check infrastructure status
├── services/              # Individual service scripts
│   ├── start-api-gateway.sh
│   ├── start-user-service.sh
│   ├── start-vehicle-service.sh
│   ├── start-product-service.sh
│   └── start-order-service.sh
└── dev/                   # Development utilities
    ├── start-all-services.sh
    ├── rebuild-restart-services.sh  # Rebuild & restart all services (keeps infrastructure)
    ├── quick-restart-service.sh     # Quick restart single service
    ├── check-status.sh
    ├── stop-all.sh
    ├── clean-build.sh
    ├── run-tests.sh
    ├── show-infra-logs.sh
    ├── show-service-logs.sh
    └── show-all-logs.sh
```

## 🎯 Common Development Tasks

### Start Everything (Recommended)
```bash
# Interactive menu with all options
./scripts/dev-start.sh

# Or directly start full environment
./scripts/docker/start-infra.sh
./scripts/dev/start-all-services.sh
```

### Start Individual Components

#### Infrastructure Only
```bash
./scripts/docker/start-infra.sh
```

#### Individual Services
```bash
./scripts/services/start-api-gateway.sh
./scripts/services/start-user-service.sh
./scripts/services/start-vehicle-service.sh
./scripts/services/start-product-service.sh
./scripts/services/start-order-service.sh
```

### Check Status
```bash
./scripts/dev/check-status.sh
```

### View Logs
```bash
# Service logs
./scripts/dev/show-service-logs.sh

# Infrastructure logs
./scripts/dev/show-infra-logs.sh

# All logs
./scripts/dev/show-all-logs.sh
```

### Stop Everything
```bash
./scripts/dev/stop-all.sh
```

### Build & Test
```bash
# Clean and build all services
./scripts/dev/clean-build.sh

# Run all tests
./scripts/dev/run-tests.sh
```

### Development Workflow Scripts
```bash
# Rebuild and restart all services (keeps infrastructure running)
./scripts/dev/rebuild-restart-services.sh

# Quick restart a single service
./scripts/dev/quick-restart-service.sh

# Interactive single service restart
./scripts/dev/quick-restart-service.sh user    # Restart user service
./scripts/dev/quick-restart-service.sh gateway # Restart API gateway
```

## 🌐 Service URLs

Once all services are running, you can access:

- **API Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081
- **Vehicle Service**: http://localhost:8082
- **Product Service**: http://localhost:8083
- **Order Service**: http://localhost:8084

### Health Check URLs

- API Gateway: http://localhost:8080/actuator/health
- User Service via Gateway: http://localhost:8080/health/user/actuator/health
- Product Service via Gateway: http://localhost:8080/health/product/actuator/health
- Vehicle Service via Gateway: http://localhost:8080/health/vehicle/actuator/health

### API Endpoints (via Gateway)

All API calls should go through the gateway at http://localhost:8080:

- Authentication: `http://localhost:8080/api/v1/auth/**`
- Users: `http://localhost:8080/api/v1/users/**`
- Cart: `http://localhost:8080/api/v1/cart/**`
- Vehicles: `http://localhost:8080/api/v1/vehicles/**`
- Products: `http://localhost:8080/api/v1/products/**`
- Categories: `http://localhost:8080/api/v1/categories/**`
- Orders: `http://localhost:8080/api/v1/orders/**`

## 🐳 Infrastructure Services

The infrastructure includes:

- **PostgreSQL Databases**:
  - User DB: localhost:5433
  - Vehicle DB: localhost:5434
  - Product DB: localhost:5435
- **Redis**: localhost:6379
- **RabbitMQ**: localhost:5672
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

## 📝 Log Files

When services are started via scripts, logs are saved to:
- `logs/api-gateway.log`
- `logs/user-service.log`
- `logs/vehicle-service.log`
- `logs/product-service.log`
- `logs/order-service.log`

## 🔧 Development Tips

1. **Always start infrastructure first** before starting services
2. **Use the interactive menu** (`./scripts/dev-start.sh`) for the best experience
3. **For quick development iterations**:
   - Use **Option 9** (Rebuild & Restart All Services) when you make changes to multiple services
   - Use **Option 10** (Quick Restart Single Service) when you modify just one service
4. **Check service status** regularly with `./scripts/dev/check-status.sh`
5. **View logs** when debugging issues
6. **Clean build** when switching branches or after dependency changes

### Development Workflow
- **Code change in one service** → Use Quick Restart (Option 10)
- **Changes in shared library or multiple services** → Use Rebuild & Restart All (Option 9)
- **Major changes or dependency updates** → Use Clean & Build All (Option 12)

### Script Features
- **Infrastructure stays running**: Rebuild scripts only restart Spring services, not databases/Redis/RabbitMQ
- **Graceful shutdown**: Services are stopped properly by PID before force-killing
- **Health checks**: Scripts wait for services to be healthy before proceeding
- **Dependency order**: Services are started in proper order (User → Vehicle → Product → Order → Gateway)

## 🚨 Troubleshooting

### Ports Already in Use
The scripts will detect if ports are in use and offer to kill existing processes.

### Services Not Starting
1. Check if infrastructure is running: `./scripts/docker/status-infra.sh`
2. Check service logs: `./scripts/dev/show-service-logs.sh`
3. Try rebuilding: `./scripts/dev/rebuild-restart-services.sh`
4. For complete rebuild: `./scripts/dev/clean-build.sh`

### Database Connection Issues
1. Ensure PostgreSQL containers are running
2. Check Docker logs: `./scripts/dev/show-infra-logs.sh`
3. Restart infrastructure: `./scripts/docker/stop-infra.sh && ./scripts/docker/start-infra.sh`

### Development Issues
- **Service won't restart**: Use `./scripts/dev/quick-restart-service.sh` for targeted restart
- **Shared library changes**: Use `./scripts/dev/rebuild-restart-services.sh` to rebuild all services
- **Stuck processes**: Scripts will force-kill processes on required ports if needed
- **Health check failures**: Check service logs for startup errors

## 🔄 Making Scripts Executable

If you get permission errors, make scripts executable:

```bash
find scripts/ -name "*.sh" -exec chmod +x {} \;
```
