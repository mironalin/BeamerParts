# BeamerParts Project Overview

## Purpose
BeamerParts is a specialized e-commerce platform for BMW aftermarket parts targeting BMW owners (Series 1-7, X1-X6) seeking replacement and upgrade parts like buttons, handles, mirror caps, spoilers, and LED lighting.

## Business Context
- **Target Market**: BMW owners in Romania seeking cosmetic and interior parts
- **Business Model**: Import affordable parts and resell with markup
- **Geographic Focus**: Romania (Romanian language, RON currency)
- **Key Features**: Guest checkout, account linking, PDF invoices, vehicle compatibility filtering

## Architecture
Microservices-based e-commerce solution using:
- **API Gateway** (Port 8080) - Spring Cloud Gateway for routing and authentication
- **User Service** (Port 8081) - User management and authentication  
- **Vehicle Service** (Port 8082) - BMW vehicle data and compatibility
- **Product Service** (Port 8083) - Product catalog and inventory
- **Order Service** (Port 8084) - Order processing and management
- **Shared Library** - Common utilities and DTOs

## Development Status
The project follows enterprise patterns with proper service boundaries, JWT authentication setup in the gateway, and comprehensive routing configuration for both authenticated and public endpoints.