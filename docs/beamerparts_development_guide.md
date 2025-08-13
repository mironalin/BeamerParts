## BeamerParts Engineering Guide

### Purpose
Authoritative, concise guide for architecture, structure, testing, and workflow across the monorepo. Supersedes the Phase 1/2/3 docs as the source of truth.

### Repo quick facts
- Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0
- Services: `api-gateway`, `user-service`, `vehicle-service`, `product-service`, `order-service`, `shared`
- Infra (local): PostgreSQL (5433/5434/5435), Redis (6379), RabbitMQ (5672/15672)
- Run scripts: see `scripts/` and `.cursorrules`

### Architectural principles
- Service per bounded context; database per service; no cross-DB FKs; business keys across services (SKU, codes)
- API split: External `/api/*` via gateway, Internal `/internal/*` service-to-service
- Async events via RabbitMQ for sync/cache updates; Redis for caching
- Consistent DTOs; validation at controller boundary; Actuator health exposed

### Product Service structure validation (current state)
- Strengths
  - Clear controller segregation: `controller/external`, `controller/admin`, `controller/internal`
  - Separate DTO namespaces: `dto/external`, `dto/internal`, `dto/shared`
  - Service layering split into `service/external` and `service/internal`
  - OpenAPI annotations and spec classes present for external/admin controllers
  - Flyway migrations align with entities and features (categories, products, variants, inventory, BMW caches, compatibility)
- Gaps to address
  - Global exception handling missing (no `@ControllerAdvice`): unify error responses and validation errors
  - Caching usage missing (no `@Cacheable`/`@CacheEvict`) despite Redis config; add for hot paths (product by SKU, category tree)
  - MapStruct declared but not used (`@Mapper` not found); introduce mappers between entities and DTOs
  - Security TODOs on admin controllers; restrict `/api/admin/*` when security arrives
  - Pagination consistency: some endpoints return lists where pagination may be needed (by-generation)
  - Response uniformity: ensure 404/validation/error payloads follow a single `ApiResponse` contract
- Actionable backlog (priority)
  1. Add `product-service` global exception handler with standardized error contract
  2. Introduce caching: `getProductBySku`, category tree, generation queries; define TTLs and evictions
  3. Add MapStruct mappers; remove ad-hoc mapping in services
  4. Normalize response shapes and pagination; document in OpenAPI
  5. Harden admin endpoints with security once gateway/user security is wired

### Testing policy (enforced)
- Mandatory tests for any new/changed endpoint (see `.cursorrules`)
  - Controllers: `@WebMvcTest` + MockMvc; include negative/validation/error paths
  - Services: unit tests with mocks; cover business branches and edge cases
  - Repositories: `@DataJpaTest` for custom queries; prefer Testcontainers for DB
- Targets: ≥70% coverage per service; ≥90% for critical business flows (inventory reserve/release, product lookup by SKU)
- Test layout mirrors main packages under `src/test/java`

### Development workflow
- Use scripts in `scripts/` for infra and service lifecycle; quick restart for single-service iterations
- DB changes: add Flyway migration + update entities; never mutate existing migrations
- After changes: run `scripts/dev/run-tests.sh` and check health endpoints
- Keep shared logic in `shared` and bump dependent services via root build when changed

### API and routing conventions
- External (gateway): `/api/v1/...` preferred; `/api/...` may alias latest
- Internal (direct service): `/internal/...`; stable contracts for inter-service usage
- Admin routes live under `/api/admin/...` and must be protected

### Observability and resilience
- Actuator enabled; keep health/info/metrics
- Apply circuit breakers/retries/timeouts on inter-service calls where applicable
- Standardize structured logging and correlation IDs as follow-up

### Consolidated roadmap (replaces phase docs)
- Milestone A: Foundation & routing
  - Status: Gateway, core services, infra, health checks in place
- Milestone B: Product catalog & compatibility (Product/Vehicle)
  - Status: Implemented; add caching, mappers, and tests; normalize pagination
- Milestone C: Cart & cross-service flows (User↔Product/Vehicle)
  - Status: Basic flows; add validation endpoints/tests and resilience
- Milestone D: Orders & payments (Order Service)
  - Status: Planned; Stripe, invoices, stock reservation with events; full test coverage

### Source-of-truth
- This guide supersedes `docs/beamerparts_phase1_guide.md`, `docs/beamerparts_phase2_guide.md`, `docs/beamerparts_phase3_guide.md`.
- Keep those for historical context; update this guide going forward.
