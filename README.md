# BeamerParts 🚗

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **BMW Aftermarket Parts E-commerce Platform** - A comprehensive microservices-based e-commerce solution for BMW interior and exterior tuning parts.

## 🌟 Project Overview

BeamerParts is a specialized e-commerce platform targeting BMW owners (Series 1-7, X1-X6) seeking replacement and upgrade parts. Built with modern microservices architecture using Spring Boot and Spring Cloud Gateway.

### 🎯 Business Domain
- **Target Market**: BMW owners seeking cosmetic and interior parts
- **Product Focus**: Buttons, handles, mirror caps, spoilers, LED lighting
- **Geographic Focus**: Romania (Romanian language, RON currency)
- **Business Model**: Import affordable parts and resell with markup

### ✨ Key Features
- 🛒 **Guest Checkout** - Order without account creation
- 🔗 **Account Linking** - Link historical orders when guests register
- 📄 **PDF Invoices** - Romanian legal compliance
- 🚗 **Vehicle Compatibility** - Parts filtered by BMW model generations
- 👨‍💼 **Admin Dashboard** - Real-time order management
- 🌐 **API Gateway** - Centralized routing and authentication

## 🏗️ Architecture

### Microservices Overview
```
┌─────────────────┐
│   API Gateway   │ ← Entry point (Port 8080)
│ Spring Cloud GW │
└─────────┬───────┘
          │
    ┌─────┼─────┐
    │     │     │
┌───▼───┐ │ ┌───▼────┐
│ User  │ │ │Vehicle │
│Service│ │ │Service │
│:8081  │ │ │ :8082  │
└───────┘ │ └────────┘
          │
    ┌─────▼─────┐
    │  Product  │
    │  Service  │
    │   :8083   │
    └───────────┘
```

### Technology Stack

**Backend Services:**
- **Java 21** - Modern Java with latest features
- **Spring Boot 3.5.4** - Microservices framework
- **Spring Cloud Gateway** - API Gateway and routing
- **Spring Security** - JWT-based authentication
- **Spring Data JPA** - Database abstraction
- **PostgreSQL** - Primary database
- **Redis** - Caching and sessions
- **RabbitMQ** - Asynchronous messaging
- **Maven** - Build and dependency management

**Infrastructure:**
- **Docker** - Containerization
- **Docker Compose** - Local development orchestration

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.8+

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/BeamerParts.git
cd BeamerParts
```

### 2. Start Development Environment
```bash
# Interactive development menu (recommended)
./start-dev.sh

# Or manually start infrastructure and services
./scripts/docker/start-infra.sh
./scripts/dev/start-all-services.sh
```

### 3. Verify Services
```bash
# Check all services are running
./scripts/dev/check-status.sh

# Test API Gateway
curl http://localhost:8080/actuator/health
```

## 📁 Project Structure

```
BeamerParts/
├── 📁 api_gateway/          # Spring Cloud Gateway (Port 8080)
├── 📁 user_service/         # User management & auth (Port 8081)
├── 📁 vehicle_service/      # BMW vehicle data (Port 8082)  
├── 📁 product_service/      # Product catalog (Port 8083)
├── 📁 order_service/        # Order management (Port 8084)
├── 📁 shared/               # Common libraries and utilities
├── 📁 scripts/              # Development and deployment scripts
├── 📁 docs/                 # Project documentation
├── 🐳 docker-compose.yml    # Local development infrastructure
├── 🚀 start-dev.sh          # Quick start script
└── 📋 pom.xml               # Parent Maven configuration
```

## 🔧 Development

### Development Scripts
The project includes comprehensive development scripts for easy management:

```bash
# Start interactive development menu
./start-dev.sh

# Individual operations
./scripts/docker/start-infra.sh              # Start databases, Redis, RabbitMQ
./scripts/dev/start-all-services.sh          # Start all microservices
./scripts/dev/rebuild-restart-services.sh    # Rebuild & restart services
./scripts/dev/quick-restart-service.sh       # Restart single service
./scripts/dev/check-status.sh                # Check service health
./scripts/dev/stop-all.sh                    # Stop everything
```

### Development Workflow
1. **Code change in one service** → Use Quick Restart
2. **Changes in multiple services** → Use Rebuild & Restart All
3. **Major changes** → Use Clean & Build All

## 🌐 API Endpoints

All API calls go through the gateway at `http://localhost:8080`:

### Authentication
```
POST /api/v1/auth/register    # User registration
POST /api/v1/auth/login       # User login
POST /api/v1/auth/refresh     # Refresh JWT token
GET  /api/v1/auth/me          # Get current user
```

### Vehicle Management
```
GET /api/v1/vehicles/series                           # BMW series list
GET /api/v1/vehicles/series/{code}/generations        # Series generations
GET /api/v1/vehicles/generations/{code}               # Generation details
GET /api/v1/vehicles/generations/{code}/products      # Compatible products
```

### Product Catalog
```
GET  /api/v1/products              # Product list
GET  /api/v1/products/{sku}        # Product details
GET  /api/v1/products/search       # Search products
GET  /api/v1/categories            # Category list
GET  /api/v1/categories/{id}/products  # Category products
```

### Shopping Cart (Authenticated)
```
GET    /api/v1/cart             # Get cart contents
POST   /api/v1/cart/items       # Add item to cart
PUT    /api/v1/cart/items/{id}  # Update cart item
DELETE /api/v1/cart/items/{id}  # Remove cart item
DELETE /api/v1/cart/clear       # Clear cart
```

### Health Monitoring
```
GET /health/user/actuator/health      # User service health
GET /health/vehicle/actuator/health   # Vehicle service health
GET /health/product/actuator/health   # Product service health
```

## 🗄️ Database Schema

### Infrastructure Services
- **PostgreSQL Databases:**
  - User DB: `localhost:5433`
  - Vehicle DB: `localhost:5434` 
  - Product DB: `localhost:5435`
- **Redis**: `localhost:6379`
- **RabbitMQ**: `localhost:5672`
- **RabbitMQ Management**: `http://localhost:15672`

## 🧪 Testing

```bash
# Run all tests
./scripts/dev/run-tests.sh

# Test API Gateway endpoints
./scripts/dev/test-api-gateway.sh

# Manual testing
curl -X GET http://localhost:8080/api/v1/vehicles/series
curl -X GET http://localhost:8080/api/v1/products
```

## 📊 Monitoring & Logs

### Log Files
Service logs are automatically saved to:
```
logs/
├── api-gateway.log
├── user-service.log
├── vehicle-service.log
├── product-service.log
└── order-service.log
```

### Viewing Logs
```bash
./scripts/dev/show-service-logs.sh    # Service logs
./scripts/dev/show-infra-logs.sh      # Infrastructure logs
./scripts/dev/show-all-logs.sh        # All logs
```

## 🔐 Security

- **JWT Authentication** - Stateless token-based auth
- **User Context Injection** - Automatic user ID injection for authenticated endpoints
- **CORS Configuration** - Configured for local development
- **Internal API Pattern** - Services communicate via `/internal/*` endpoints

## 🛠️ Configuration

### Service Configuration
Each service has its own `application.yml` with:
- Database connections
- Service discovery
- Health checks
- Logging configuration

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_USERNAME=beamerparts_user
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_USERNAME=beamerparts
RABBITMQ_PASSWORD=password
```

## 🚨 Troubleshooting

### Common Issues
1. **Port conflicts** - Scripts auto-detect and kill conflicting processes
2. **Database connection** - Ensure infrastructure is running first
3. **Service startup** - Check logs and health endpoints
4. **Build failures** - Try clean build: `./scripts/dev/clean-build.sh`

### Debug Commands
```bash
./scripts/docker/status-infra.sh     # Check infrastructure
./scripts/dev/check-status.sh        # Check all services
docker ps                            # Check running containers
lsof -i :8080                        # Check port usage
```

## 📚 Documentation

Comprehensive documentation is available in the `docs/` folder:
- [Project Structure Guide](docs/beamerparts_project_structure.md)
- [Bootstrap Guide](docs/beamerparts_bootstrap_guide.md)
- [Phase Implementation Guides](docs/)
- [Dependencies Guide](docs/beamerparts_dependencies_guide.md)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Development Guidelines
- Follow Spring Boot best practices
- Write unit tests for new features
- Update documentation for API changes
- Use the provided development scripts

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Spring Cloud Gateway for robust API gateway solution
- BMW community for inspiration

---

## 🔗 Links

- **Development Scripts**: [scripts/README.md](scripts/README.md)
- **API Documentation**: [docs/beamerparts_documentation.md](docs/beamerparts_documentation.md)
- **Project Architecture**: [docs/beamerparts_project_structure.md](docs/beamerparts_project_structure.md)

---

**Made with ❤️ for BMW enthusiasts**
