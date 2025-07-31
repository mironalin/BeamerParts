# BeamerParts - BMW Aftermarket Parts E-commerce Platform

## Project Overview

A specialized e-commerce platform for BMW aftermarket interior and exterior tuning parts. The system consists of a private REST API backend and two frontend applications: a customer-facing store and an admin dashboard for order management.

### Business Domain
- **Target Market**: BMW owners (Series 1-7, X1-X6) seeking replacement/upgrade parts
- **Product Focus**: Cosmetic and interior parts only - buttons, handles, mirror caps, spoilers, LED lighting
- **Business Model**: Import affordable parts from suppliers (AliExpress) and resell with markup
- **Geographic Focus**: Romania (Romanian language, RON currency, local shipping)

### Key Business Requirements
- **Guest Checkout**: Users can order without accounts, but must provide email
- **Account Linking**: When guests create accounts with previously used email, link historical orders
- **Invoice Generation**: Professional PDF invoices with Romanian legal compliance
- **Vehicle Compatibility**: Parts must be filtered by specific BMW model generations
- **Admin Management**: Real-time order monitoring and fulfillment interface

## System Architecture

### True Microservices Architecture
```
Frontend Applications:
├── Customer Store (Next.js/Angular) - SEO-optimized e-commerce
└── Admin Dashboard (React/Angular) - Order management interface
                    │
            ┌───────▼───────┐
            │  API Gateway  │
            │ (Spring Cloud │
            │   Gateway)   │
            │  Port: 8080   │
            └───────┬───────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
┌───────▼──────┐ ┌──▼─────┐ ┌───▼──────────┐
│ User Service │ │Vehicle │ │   Product    │
│ Port: 8081   │ │Service │ │   Service    │
│ DB: user_db  │ │Port:82 │ │ Port: 8083   │
└──────────────┘ │DB:veh_db│ │DB:product_db │
                 └────────┘ └──────────────┘
        │                           │
┌───────▼──────┐              ┌─────▼──────────┐
│Order Service │              │ Notification   │
│ Port: 8084   │              │   Service      │
│ DB: order_db │              │ Port: 8085     │
└──────────────┘              │DB:notification │
                               └────────────────┘

Message Queue Layer:
└── RabbitMQ - Event-driven inter-service communication

Shared Infrastructure:
├── Redis Cluster - Distributed caching
├── Docker - Service containerization  
├── Kubernetes - Container orchestration
├── Terraform - Infrastructure as code
└── Cloud Platform - Managed databases and services
```

### Service Boundaries & Database Distribution
```
User Service (user_db):
├── User authentication and profiles
├── JWT token management
├── Shopping cart (user-specific)
└── User vehicle preferences

Vehicle Service (vehicle_db):
├── BMW hierarchy (Series → Generations)
├── Compatibility registry master data
└── Vehicle lookup and validation

Product Service (product_db):
├── Product catalog and categories
├── Inventory management
├── BMW compatibility mapping
├── Cached vehicle data for performance
└── Product images and variants

Order Service (order_db):
├── Order processing and management
├── Payment integration
├── Invoice generation
└── Order status tracking

Notification Service (notification_db):
├── Email templates and sending
├── SMS notifications (optional)
├── Real-time WebSocket updates
└── Communication history
```

### Core Domain Models (Distributed Across Services)

#### User Service Domain
```
Users (Authentication & Profiles)
├── User Accounts (email, password, profile info)
├── Refresh Tokens (JWT token management)
├── User Vehicles (BMW preferences using codes, not foreign keys)
└── Shopping Cart (product references via SKUs)
```

#### Vehicle Service Domain  
```
BMW Hierarchy (Master Data)
├── BMW Series (1, 2, 3, 4, 5, 6, 7, X1-X6)
├── BMW Generations (E46, E90, F30, G20, etc.)
└── Compatibility Registry (generation-to-product mapping via codes)
```

#### Product Service Domain
```
Product Catalog
├── Categories (Interior, Exterior, Lighting)
├── Products (with SKUs as primary identifiers)
├── Product Images (thumbnail, medium, large)
├── Product Variants (colors, materials)
├── Inventory Management (stock levels, reservations)
├── BMW Cache (synchronized copy of vehicle data)
└── Product Compatibility (uses cached BMW data)
```

#### Order Service Domain
```
Order Processing
├── Orders (customer info via user codes or guest email)
├── Order Items (product references via SKUs)
├── Payments (Stripe integration)
├── Invoices (PDF generation with Romanian legal requirements)
└── Status History (order lifecycle tracking)
```

#### Cross-Service Communication Pattern
```
Business Keys Instead of Foreign Keys:
- User references: email or user_code
- Product references: SKU (BMW-F30-AC-001)
- BMW references: generation_code (F30, E90)
- Order references: order_number

Event-Driven Synchronization:
- Vehicle data changes → Events → Product cache updates
- User registration → Events → Welcome email
- Order placement → Events → Inventory reduction
```

## Technology Stack Specification

### Backend Requirements
- **Framework**: Spring Boot 3.2+ with comprehensive ecosystem
  - Spring Security for JWT authentication and role-based authorization
  - Spring Data JPA for database operations with PostgreSQL
  - Spring Cloud Gateway for API gateway functionality
  - Spring Cache for Redis integration
  - Spring Validation for request/response validation
  - Spring Boot Actuator for monitoring and health checks

- **Database**: PostgreSQL for ACID compliance and complex relationships
  - Optimized for e-commerce queries (product filtering, order management)
  - Proper indexing strategy for search and compatibility queries
  - Connection pooling with HikariCP

- **Caching**: Redis for performance optimization
  - Product catalog caching
  - User session storage
  - Shopping cart persistence

- **Message Queue**: RabbitMQ for event-driven architecture
  - Order processing events
  - Inventory updates
  - Email notifications
  - Admin alerts

### Frontend Requirements
- **Customer Store**: Next.js or Angular for SEO optimization
  - Server-side rendering for product pages
  - Vehicle-specific product filtering
  - Responsive design for mobile/desktop

- **Admin Dashboard**: React or Angular for real-time management
  - WebSocket integration for live order updates
  - Product management interface
  - Order fulfillment workflow
  - Analytics and reporting

### Infrastructure Requirements
- **Containerization**: Docker for all services
- **Orchestration**: Kubernetes for production deployment
- **Infrastructure as Code**: Terraform for cloud resource management
- **Cloud Services**: Managed databases, Redis, file storage, CDN
- **Monitoring**: Application metrics, logging, and alerting

## Business Logic Specifications

### Product Management
- Products must be assigned to at least one BMW generation
- Stock quantities tracked and updated automatically on orders
- Images required (minimum 1, maximum 10 per product)
- Prices in RON with 2 decimal precision
- SKU format: BMW-{GENERATION}-{CATEGORY}-{NUMBER}

### Order Processing Workflow
1. **Cart Management**: Add/remove items with real-time stock validation
2. **Checkout Process**: Collect shipping info, validate cart, calculate totals
3. **Payment Processing**: Stripe integration with webhook handling
4. **Order Confirmation**: Generate invoice, send emails, update inventory
5. **Admin Fulfillment**: Order status updates, shipping tracking
6. **Completion**: Delivery confirmation, customer feedback

### Guest to Account Migration
- Guest orders stored with email address only
- When user registers with existing email, automatically link previous orders
- Migration tracking for audit purposes
- Email notification about recovered order history

### Invoice Generation Requirements
- **Company Information**: Name, address, Romanian tax registration numbers
- **Legal Compliance**: Sequential numbering, VAT handling, return policy
- **Customer Details**: Name, email, billing/shipping addresses
- **Order Information**: Products, quantities, prices, totals, payment method
- **Format**: Professional PDF template with company branding

## API Design Principles

### RESTful Architecture
- Resource-based URLs (`/api/products`, `/api/orders`)
- HTTP methods for operations (GET, POST, PUT, DELETE)
- Consistent JSON response format with proper status codes
- Pagination for list endpoints
- Filtering and sorting capabilities

### Authentication & Authorization
- JWT tokens with RS256 signing
- Access tokens (15 minutes) + Refresh tokens (7 days)
- Role-based access control (Customer, Admin, Super Admin)
- Rate limiting per endpoint and user role

### Error Handling Strategy
- Consistent error response format with correlation IDs
- Internationalization support (Romanian/English)
- Proper HTTP status codes
- Detailed validation error messages
- Structured logging for debugging

## Data Management Requirements

### Distributed Database Strategy
- **Database per Service**: Each microservice owns its data completely
- **No Cross-Service Foreign Keys**: Use business identifiers (codes, SKUs)
- **Event-Driven Synchronization**: RabbitMQ for data consistency
- **Strategic Data Duplication**: Cache frequently accessed cross-service data

### Database Schema Principles
- **Service-Specific Normalization**: Optimize within service boundaries
- **Business Key References**: SKUs, codes, emails instead of numeric IDs
- **Eventual Consistency**: Accept brief delays for cross-service data updates
- **Audit Trails**: Track changes within each service boundary

### Cross-Service Data Consistency
```yaml
Master Data Ownership:
  - User Service: User accounts, authentication
  - Vehicle Service: BMW hierarchy, compatibility rules  
  - Product Service: Product catalog, inventory
  - Order Service: Order processing, payments

Data Synchronization Patterns:
  - BMW data changes → Vehicle Service event → Product Service cache update
  - User registration → User Service event → Notification Service welcome email
  - Order placement → Order Service event → Product Service inventory update
  - Stock changes → Product Service event → Admin Dashboard notification
```

### Performance Optimization
- **Service-Level Caching**: Redis for each service's frequently accessed data
- **Strategic Data Duplication**: BMW cache in Product Service for fast compatibility queries
- **Connection Pooling**: HikariCP configured per service database
- **Read Replicas**: For analytics and reporting queries within services

### Inter-Service Communication
```yaml
Synchronous Communication (REST):
  - API Gateway → Individual Services
  - Service-to-Service calls for immediate data needs
  - Circuit breaker pattern for fault tolerance

Asynchronous Communication (Events):
  - RabbitMQ message broker
  - Event publishing for data changes
  - Eventual consistency for non-critical updates
  - Dead letter queues for failed message handling
```

## Security Requirements

### API Security
- HTTPS enforcement in production
- Input validation and sanitization
- SQL injection prevention through parameterized queries
- XSS protection for user-generated content
- CORS configuration for frontend domains

### Data Protection
- Password hashing with BCrypt
- Sensitive data encryption at rest
- PCI DSS considerations for payment data
- GDPR compliance for customer data
- Audit logging for admin actions

### Access Control
- Multi-factor authentication for admin accounts
- Session management and timeout policies
- IP whitelisting for admin access (optional)
- Regular security updates and vulnerability scanning

## Integration Requirements

### Payment Processing
- Stripe integration for card payments
- Webhook handling for payment events
- Refund and chargeback management
- Multiple payment methods support
- Fraud detection and prevention

### Email Communications
- Transactional emails (order confirmations, status updates)
- Template management for different email types
- Delivery tracking and bounce handling
- Marketing email capabilities (future)
- Multi-language support

### File Management
- Image upload and processing for products
- Multiple image sizes (thumbnail, medium, large)
- CDN integration for fast delivery
- File type validation and security
- Automatic image optimization

## Performance & Scalability Requirements

### Performance Targets
- API response time: < 200ms (95th percentile)
- Database query time: < 100ms average
- Cache hit rate: > 80% for product catalog
- Page load time: < 2 seconds for product pages
- Uptime: 99.9% availability

### Scalability Considerations
- Horizontal scaling for stateless services
- Database connection pooling optimization
- Load balancing for high availability
- Auto-scaling based on demand
- Performance monitoring and alerting

## Quality Assurance Requirements

### Testing Strategy
- Unit tests for business logic (70% coverage)
- Integration tests for database operations
- Contract tests for API endpoints
- End-to-end tests for critical user journeys
- Performance testing for load scenarios

### Code Quality
- Static code analysis and linting
- Code review process for all changes
- Documentation for complex business logic
- Consistent coding standards and formatting
- Dependency vulnerability scanning

## Deployment & Operations

### Environment Strategy
- **Development**: Docker Compose for local development
- **Staging**: Kubernetes cluster with production-like setup
- **Production**: Managed cloud services with high availability

### Infrastructure Management
- Infrastructure as Code with Terraform
- Automated deployment pipelines
- Blue-green deployment strategy
- Database migration automation
- Rollback procedures

### Monitoring & Observability
- Application performance monitoring
- Business metrics tracking (conversion rates, sales)
- Error tracking and alerting
- Log aggregation and analysis
- Health checks and uptime monitoring

## Development Roadmap

### Phase 1: Microservices Foundation (6-8 weeks)
**Objective**: Establish true microservices architecture with separate databases

**Deliverables**:
- Three core microservices: User, Vehicle, and Product Services
- API Gateway with Spring Cloud Gateway for routing and security
- Separate PostgreSQL databases for each service  
- RabbitMQ message broker for inter-service communication
- Docker Compose setup for local development environment
- Service-to-service authentication and communication
- Event-driven data synchronization between services
- Basic CRUD operations within each service boundary

**Key Milestones**:
- ✅ All three services deployable independently
- ✅ API Gateway routing requests correctly
- ✅ Cross-service communication working (REST + Events)
- ✅ Database per service with proper isolation
- ✅ BMW vehicle data synchronized between Vehicle and Product services
- ✅ User authentication working across service boundaries

### Phase 2: Core Business Logic (4-5 weeks)
**Objective**: Implement essential e-commerce functionality within microservices

**Deliverables**:
- Shopping cart functionality in User Service
- Product catalog with BMW compatibility in Product Service
- Inventory management with cross-service stock tracking
- User registration and JWT authentication across services
- Basic product search and filtering capabilities
- Event-driven inventory updates
- Error handling and service resilience patterns

**Key Milestones**:
- ✅ Users can browse products filtered by BMW compatibility
- ✅ Shopping cart operations work across services
- ✅ Stock levels synchronized between services
- ✅ Service failures handled gracefully with circuit breakers

### Phase 3: Order Processing & Payment (4-5 weeks)
**Objective**: Add Order Service and complete the purchase workflow

**Deliverables**:
- Order Service with separate database for order management
- Guest checkout functionality with email-based ordering
- Stripe payment integration with webhook handling
- Invoice generation with Romanian legal requirements
- Cross-service order processing workflow
- Email notification system for order confirmations
- Event-driven inventory updates when orders are placed
- Order status tracking across service boundaries

**Key Milestones**:
- ✅ Complete guest and user checkout processes working
- ✅ Payment processing integrated with order workflow
- ✅ Cross-service inventory updates on order placement
- ✅ Professional invoices generated and emailed

### Phase 4: Admin Dashboard & Management (3-4 weeks)
**Objective**: Build comprehensive admin interface for order and product management

**Deliverables**:
- React/Angular admin dashboard application
- Real-time order monitoring with WebSocket integration across services
- Product management interface with cross-service data updates
- Order fulfillment workflow with status updates
- Customer communication tools
- Analytics dashboard aggregating data from multiple services
- Admin user management and role-based permissions

**Key Milestones**:
- ✅ Admin can monitor orders from all services in real-time
- ✅ Product management updates propagate correctly across services
- ✅ Order fulfillment workflow complete with status tracking

### Phase 5: Account Linking & Advanced Features (3-4 weeks)
**Objective**: Implement advanced e-commerce features with microservices complexity

**Deliverables**:
- Guest order linking when accounts are created (cross-service data migration)
- Advanced product search and filtering with cross-service data
- Vehicle-specific product recommendations
- Enhanced caching strategy with Redis Cluster
- Comprehensive event-driven architecture with RabbitMQ
- Service resilience patterns (circuit breakers, retries, timeouts)
- API documentation for all services (OpenAPI/Swagger)

**Key Milestones**:
- ✅ Guest orders automatically linked to new accounts across services
- ✅ Advanced search working with distributed data
- ✅ All services resilient to failures and network issues

### Phase 6: Customer Frontend (5-6 weeks)
**Objective**: Build SEO-optimized customer-facing store integrating all microservices

**Deliverables**:
- Next.js/Angular customer store application
- SEO-optimized product and category pages
- Vehicle selection and compatible parts filtering
- Responsive design for mobile and desktop
- Shopping cart and checkout user interface integrating multiple services
- User account management with cross-service data
- Blog/content management for SEO

**Key Milestones**:
- ✅ Customers can browse and purchase products seamlessly
- ✅ SEO optimization driving organic traffic
- ✅ Mobile-responsive experience across all features

### Phase 7: Production Deployment & Scaling (4-5 weeks)
**Objective**: Deploy microservices to production with enterprise-grade infrastructure

**Deliverables**:
- Terraform infrastructure as code for cloud deployment
- Kubernetes cluster setup with service mesh (optional)
- CI/CD pipeline for independent service deployments
- Production monitoring and logging (Prometheus, Grafana, ELK)
- Service discovery and load balancing
- SSL certificates and domain configuration
- Database backup and disaster recovery for multiple databases
- Performance optimization and load testing

**Key Milestones**:
- ✅ All microservices deployed independently in production
- ✅ Automated deployment pipeline for each service
- ✅ Comprehensive monitoring and alerting across all services

### Phase 8: Optimization & Growth (Ongoing)
**Objective**: Continuous improvement and advanced microservices patterns

**Future Enhancements**:
- Service mesh implementation (Istio) for advanced traffic management
- Advanced analytics with data aggregation across services
- CQRS and Event Sourcing patterns for complex business logic
- Distributed tracing for cross-service request monitoring
- Advanced caching strategies with distributed cache invalidation
- Mobile application development
- Multi-language support with distributed content management
- Customer loyalty program with cross-service integration

## Microservices Learning Considerations

### **Increased Complexity**
Starting with true microservices architecture significantly increases the learning curve:
- **Distributed Systems Challenges**: Network failures, eventual consistency, service discovery
- **Development Overhead**: More complex local development setup with multiple databases
- **Debugging Complexity**: Tracing requests across multiple services
- **Testing Challenges**: Integration testing across service boundaries

### **Enhanced Learning Value**
The increased complexity provides valuable enterprise development experience:
- **Real-World Patterns**: Learn how large companies actually build systems
- **Problem-Solving Skills**: Handle distributed systems challenges from the start
- **Architecture Understanding**: Deep comprehension of service boundaries and data flow
- **Career Preparation**: Skills directly applicable to senior developer roles

### **Success Strategy**
- **Start Simple**: Implement basic functionality in each service before adding complexity
- **One Service at a Time**: Perfect one service before moving to the next
- **Focus on Patterns**: Learn microservices patterns rather than rushing features
- **Document Everything**: Track service interactions and data flows
- **Embrace Failure**: Use failures as learning opportunities for resilience patterns

## Success Metrics

### Technical Metrics
- **Performance**: API response times under target thresholds
- **Reliability**: 99.9% uptime with minimal service disruptions
- **Security**: Zero data breaches or security incidents
- **Quality**: Comprehensive test coverage and code quality scores

### Business Metrics
- **Conversion**: Guest-to-customer conversion rate > 15%
- **Sales**: Monthly revenue growth tracking
- **Customer Satisfaction**: Low return rates and positive reviews
- **Operational Efficiency**: Order fulfillment time optimization

### Learning Objectives
- **Spring Boot Mastery**: Deep understanding of enterprise Java development
- **Cloud Architecture**: Hands-on experience with modern infrastructure
- **E-commerce Domain**: Complete understanding of online retail systems
- **DevOps Practices**: Full-stack deployment and operations experience

---

**This specification provides comprehensive guidance for building BeamerParts while maintaining flexibility for implementation choices. Each phase builds upon the previous, ensuring steady progress toward a production-ready e-commerce platform.**