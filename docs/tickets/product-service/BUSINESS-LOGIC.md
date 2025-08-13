# Product Service Business Logic Documentation

## 🎯 **Service Purpose**
Product Service manages the complete product catalog, inventory operations, and BMW compatibility validation for the BeamerParts e-commerce platform. It serves as the central hub for product data and inventory coordination across the platform.

## 🏗️ **Core Domain Concepts**

### **Product Catalog Hierarchy**
```
Category Hierarchy
├── Engine (categoryId: 1)
│   ├── Air Intake (categoryId: 2)
│   ├── Exhaust (categoryId: 3)
│   └── Fuel System (categoryId: 4)
├── Suspension (categoryId: 5)
│   ├── Coilovers (categoryId: 6)
│   └── Springs (categoryId: 7)
└── Exterior (categoryId: 8)
    ├── Body Kits (categoryId: 9)
    └── Lighting (categoryId: 10)
```

### **Key Business Entities**

#### **Product**
- Central entity representing BMW parts and accessories
- Contains SKU, pricing, descriptions, and specifications
- Links to categories, variants, and BMW compatibility
- Manages product lifecycle (active, discontinued, coming soon)

#### **ProductVariant**
- Product variations (size, color, material options)
- Independent pricing and inventory tracking
- BMW-specific configurations (left/right drive, market-specific)
- SKU suffix system for variant identification

#### **Category** 
- Hierarchical product organization
- Support for nested categories (parent-child relationships)
- Category-level BMW compatibility rules
- Display ordering and navigation structure

#### **Inventory**
- Real-time stock tracking per product/variant
- Stock reservation system for order processing
- Reorder point management and low stock alerts
- Stock movement tracking and audit trail

#### **BmwCompatibility**
- Product-to-BMW generation compatibility mapping
- Integration with vehicle-service for validation
- Compatibility notes and verification status
- Support for universal and specific compatibility

## 🧠 **Critical Business Logic**

### **Product Catalog Management**
**Purpose**: Comprehensive product lifecycle and catalog organization.

**Product Validation Logic**:
```java
public class ProductCatalogDomainService {
    
    public Product createProduct(CreateProductRequest request) {
        // 1. Validate SKU uniqueness across all products
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException("SKU already exists: " + request.getSku());
        }
        
        // 2. Validate category assignment and hierarchy
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
        
        if (!category.isLeafCategory()) {
            throw new InvalidCategoryException("Products can only be assigned to leaf categories");
        }
        
        // 3. Validate pricing and business rules
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPriceException("Product price must be positive");
        }
        
        // 4. Generate product slug for SEO
        String slug = generateUniqueSlug(request.getName());
        
        // 5. Create product with business defaults
        Product product = Product.builder()
            .sku(request.getSku())
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .category(category)
            .slug(slug)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        Product savedProduct = productRepository.save(product);
        
        // 6. Initialize inventory record
        Inventory inventory = Inventory.builder()
            .product(savedProduct)
            .currentStock(0)
            .reservedStock(0)
            .reorderPoint(request.getReorderPoint())
            .build();
        
        inventoryRepository.save(inventory);
        
        // 7. Publish product created event
        eventPublisher.publishProductCreated(ProductCreatedEvent.from(savedProduct));
        
        return savedProduct;
    }
    
    private String generateUniqueSlug(String name) {
        String baseSlug = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-");
        
        String slug = baseSlug;
        int counter = 1;
        
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        
        return slug;
    }
}
```

### **Inventory Management Logic**
**Purpose**: Real-time stock tracking with reservation system for order processing.

**Stock Reservation Flow**:
```java
public class InventoryDomainService {
    
    @Transactional
    public StockReservationResult reserveStock(StockReservationRequest request) {
        List<StockReservationItem> items = request.getItems();
        List<StockReservation> reservations = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        
        for (StockReservationItem item : items) {
            try {
                StockReservation reservation = reserveProductStock(
                    item.getProductSku(), 
                    item.getQuantity(), 
                    request.getOrderId(),
                    request.getReservationTimeoutMinutes()
                );
                reservations.add(reservation);
            } catch (InsufficientStockException e) {
                failures.add(e.getMessage());
            }
        }
        
        if (!failures.isEmpty()) {
            // Rollback any successful reservations
            reservations.forEach(this::releaseReservation);
            throw new BulkStockReservationException("Stock reservation failed", failures);
        }
        
        return StockReservationResult.builder()
            .successful(true)
            .reservations(reservations)
            .reservationIds(reservations.stream()
                .collect(toMap(r -> r.getProductSku(), r -> r.getId().toString())))
            .build();
    }
    
    private StockReservation reserveProductStock(String productSku, Integer quantity, 
            Long orderId, Integer timeoutMinutes) {
        
        Inventory inventory = inventoryRepository.findByProductSku(productSku)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productSku));
        
        // Check available stock (current - reserved)
        Integer availableStock = inventory.getCurrentStock() - inventory.getReservedStock();
        
        if (availableStock < quantity) {
            throw new InsufficientStockException(
                String.format("Insufficient stock for %s. Available: %d, Requested: %d", 
                    productSku, availableStock, quantity));
        }
        
        // Create reservation record
        StockReservation reservation = StockReservation.builder()
            .inventory(inventory)
            .productSku(productSku)
            .quantityReserved(quantity)
            .orderId(orderId)
            .reservedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(timeoutMinutes))
            .status(ReservationStatus.ACTIVE)
            .build();
        
        StockReservation savedReservation = stockReservationRepository.save(reservation);
        
        // Update inventory reserved stock
        inventory.setReservedStock(inventory.getReservedStock() + quantity);
        inventoryRepository.save(inventory);
        
        // Create stock movement for audit
        createStockMovement(inventory, StockMovementType.RESERVED, quantity, 
            "Reserved for order " + orderId);
        
        return savedReservation;
    }
    
    @Transactional
    public void confirmReservation(String reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        
        if (!reservation.getStatus().equals(ReservationStatus.ACTIVE)) {
            throw new InvalidReservationStateException("Reservation is not active: " + reservationId);
        }
        
        Inventory inventory = reservation.getInventory();
        
        // Reduce actual stock and reserved stock
        inventory.setCurrentStock(inventory.getCurrentStock() - reservation.getQuantityReserved());
        inventory.setReservedStock(inventory.getReservedStock() - reservation.getQuantityReserved());
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        
        inventoryRepository.save(inventory);
        stockReservationRepository.save(reservation);
        
        // Create stock movement for audit
        createStockMovement(inventory, StockMovementType.SOLD, 
            reservation.getQuantityReserved(), "Confirmed sale for order " + reservation.getOrderId());
        
        // Check for low stock alert
        if (inventory.getCurrentStock() <= inventory.getReorderPoint()) {
            eventPublisher.publishLowStockAlert(LowStockAlertEvent.from(inventory));
        }
    }
}
```

### **BMW Compatibility Logic**
**Purpose**: Validate product compatibility with BMW vehicles using vehicle-service integration.

**Compatibility Validation**:
```java
public class BmwCompatibilityDomainService {
    
    public CompatibilityValidationResult validateProductCompatibility(
            String productSku, String bmwGenerationCode) {
        
        // 1. Get product compatibility records
        List<ProductCompatibility> compatibilities = 
            productCompatibilityRepository.findByProductSku(productSku);
        
        if (compatibilities.isEmpty()) {
            return CompatibilityValidationResult.unknown("No compatibility data available");
        }
        
        // 2. Check direct compatibility match
        Optional<ProductCompatibility> directMatch = compatibilities.stream()
            .filter(comp -> comp.getBmwGenerationCode().equals(bmwGenerationCode))
            .findFirst();
        
        if (directMatch.isPresent()) {
            ProductCompatibility match = directMatch.get();
            return CompatibilityValidationResult.builder()
                .compatible(match.isCompatible())
                .confidence(CompatibilityConfidence.HIGH)
                .source("Direct compatibility record")
                .notes(match.getNotes())
                .verificationStatus(match.getVerificationStatus())
                .build();
        }
        
        // 3. Check universal compatibility
        Optional<ProductCompatibility> universalMatch = compatibilities.stream()
            .filter(comp -> comp.isUniversalCompatibility())
            .findFirst();
        
        if (universalMatch.isPresent()) {
            return CompatibilityValidationResult.builder()
                .compatible(true)
                .confidence(CompatibilityConfidence.HIGH)
                .source("Universal compatibility")
                .notes("Compatible with all BMW models")
                .build();
        }
        
        // 4. Check series-level compatibility via vehicle-service
        BmwGenerationDto generation = vehicleServiceClient.getGeneration(bmwGenerationCode);
        if (generation != null) {
            return checkSeriesCompatibility(compatibilities, generation.getSeriesCode());
        }
        
        return CompatibilityValidationResult.unknown("Unable to determine compatibility");
    }
    
    private CompatibilityValidationResult checkSeriesCompatibility(
            List<ProductCompatibility> compatibilities, String seriesCode) {
        
        // Check if any compatibility record matches the series
        boolean hasSeriesCompatibility = compatibilities.stream()
            .anyMatch(comp -> {
                BmwGenerationDto compGeneration = vehicleServiceClient.getGeneration(comp.getBmwGenerationCode());
                return compGeneration != null && compGeneration.getSeriesCode().equals(seriesCode);
            });
        
        if (hasSeriesCompatibility) {
            return CompatibilityValidationResult.builder()
                .compatible(true)
                .confidence(CompatibilityConfidence.MEDIUM)
                .source("Series-level compatibility")
                .notes("Compatible with " + seriesCode + " series")
                .build();
        }
        
        return CompatibilityValidationResult.notCompatible("No series compatibility found");
    }
    
    public BulkCompatibilityResult validateBulkCompatibility(
            List<CompatibilityCheckRequest> requests) {
        
        // Group requests by generation for efficient processing
        Map<String, List<CompatibilityCheckRequest>> groupedRequests = 
            requests.stream().collect(groupingBy(CompatibilityCheckRequest::getBmwGenerationCode));
        
        List<CompatibilityValidationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<CompatibilityCheckRequest>> entry : groupedRequests.entrySet()) {
            String generationCode = entry.getKey();
            List<CompatibilityCheckRequest> generationRequests = entry.getValue();
            
            // Cache generation data for this batch
            BmwGenerationDto generation = vehicleServiceClient.getGeneration(generationCode);
            
            for (CompatibilityCheckRequest request : generationRequests) {
                CompatibilityValidationResult result = validateProductCompatibilityWithCache(
                    request.getProductSku(), generationCode, generation);
                results.add(result);
            }
        }
        
        return BulkCompatibilityResult.builder()
            .results(results)
            .totalRequests(requests.size())
            .processingTimeMs(System.currentTimeMillis() - startTime)
            .build();
    }
}
```

### **Product Search and Filtering Logic**
**Purpose**: Advanced search capabilities with BMW compatibility and category filtering.

**Search Implementation**:
```java
public class ProductSearchService {
    
    public Page<ProductSearchResultDto> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> root = query.from(Product.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Active products only
        predicates.add(cb.isTrue(root.get("isActive")));
        
        // Text search
        if (criteria.getSearchTerm() != null) {
            String searchPattern = "%" + criteria.getSearchTerm().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("name")), searchPattern),
                cb.like(cb.lower(root.get("description")), searchPattern),
                cb.like(cb.lower(root.get("sku")), searchPattern)
            ));
        }
        
        // Category filtering (including subcategories)
        if (criteria.getCategoryId() != null) {
            Join<Product, Category> categoryJoin = root.join("category");
            
            if (criteria.isIncludeSubcategories()) {
                // Get all subcategory IDs
                List<Long> categoryIds = categoryService.getCategoryWithSubcategoryIds(criteria.getCategoryId());
                predicates.add(categoryJoin.get("id").in(categoryIds));
            } else {
                predicates.add(cb.equal(categoryJoin.get("id"), criteria.getCategoryId()));
            }
        }
        
        // Price range filtering
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
        }
        
        // BMW compatibility filtering
        if (criteria.getBmwGenerationCode() != null) {
            Join<Product, ProductCompatibility> compatibilityJoin = root.join("compatibilities");
            predicates.add(cb.or(
                cb.equal(compatibilityJoin.get("bmwGenerationCode"), criteria.getBmwGenerationCode()),
                cb.isTrue(compatibilityJoin.get("isUniversalCompatibility"))
            ));
        }
        
        // Stock availability filtering
        if (criteria.isInStockOnly()) {
            Join<Product, Inventory> inventoryJoin = root.join("inventory");
            predicates.add(cb.greaterThan(
                cb.diff(inventoryJoin.get("currentStock"), inventoryJoin.get("reservedStock")), 0));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<javax.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(root.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(sortOrder.getProperty())));
                }
            }
            query.orderBy(orders);
        } else {
            // Default sorting by name
            query.orderBy(cb.asc(root.get("name")));
        }
        
        // Execute query
        TypedQuery<Product> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<Product> products = typedQuery.getResultList();
        long totalCount = countProductsWithCriteria(criteria);
        
        // Convert to DTOs with real-time inventory data
        List<ProductSearchResultDto> resultDtos = products.stream()
            .map(this::convertToSearchResult)
            .collect(toList());
        
        return new PageImpl<>(resultDtos, pageable, totalCount);
    }
    
    private ProductSearchResultDto convertToSearchResult(Product product) {
        Inventory inventory = product.getInventory();
        Integer availableStock = inventory.getCurrentStock() - inventory.getReservedStock();
        
        return ProductSearchResultDto.builder()
            .id(product.getId())
            .sku(product.getSku())
            .name(product.getName())
            .price(product.getPrice())
            .categoryName(product.getCategory().getName())
            .imageUrl(product.getMainImageUrl())
            .isInStock(availableStock > 0)
            .availableQuantity(availableStock)
            .isLowStock(inventory.getCurrentStock() <= inventory.getReorderPoint())
            .build();
    }
}
```

## 🔄 **Event-Driven Architecture**

### **Events Published**
```java
// Product lifecycle events
public class ProductCreatedEvent {
    private Long productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private Long categoryId;
    private LocalDateTime createdAt;
}

public class ProductUpdatedEvent {
    private Long productId;
    private String sku;
    private Map<String, Object> changedFields;
    private LocalDateTime updatedAt;
}

public class ProductPriceChangedEvent {
    private Long productId;
    private String sku;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private LocalDateTime changedAt;
}

// Inventory events
public class LowStockAlertEvent {
    private Long inventoryId;
    private String productSku;
    private Integer currentStock;
    private Integer reorderPoint;
    private LocalDateTime alertTime;
}

public class StockReservationEvent {
    private String reservationId;
    private String productSku;
    private Integer quantity;
    private Long orderId;
    private LocalDateTime reservedAt;
}
```

### **Events Consumed**
```java
// From vehicle-service
@EventListener
public void handleBmwDataUpdated(BmwDataUpdatedEvent event) {
    // Update BMW compatibility cache
    // Refresh compatibility validation data
    bmwCompatibilityService.refreshCompatibilityCache(event.getGenerationCode());
}

// From order-service
@EventListener
public void handleOrderCancelled(OrderCancelledEvent event) {
    // Release any stock reservations for the cancelled order
    inventoryService.releaseOrderReservations(event.getOrderId());
}
```

## 📊 **Performance Optimization Logic**

### **Caching Strategy**
```java
@Service
public class ProductCacheService {
    
    @Cacheable(value = "products", key = "#sku")
    public ProductDto getCachedProduct(String sku) {
        Product product = productRepository.findBySku(sku)
            .orElseThrow(() -> new ProductNotFoundException(sku));
        return productMapper.toDto(product);
    }
    
    @Cacheable(value = "inventory", key = "#productSku")
    public InventoryDto getCachedInventory(String productSku) {
        Inventory inventory = inventoryRepository.findByProductSku(productSku)
            .orElseThrow(() -> new ProductNotFoundException(productSku));
        return inventoryMapper.toDto(inventory);
    }
    
    @CacheEvict(value = {"products", "inventory"}, key = "#productSku")
    public void invalidateProductCache(String productSku) {
        log.info("Invalidating cache for product: {}", productSku);
    }
}
```

This comprehensive business logic documentation provides deep domain knowledge for the product catalog, inventory management, BMW compatibility validation, and search functionality that serves as the foundation for the entire BeamerParts platform.
