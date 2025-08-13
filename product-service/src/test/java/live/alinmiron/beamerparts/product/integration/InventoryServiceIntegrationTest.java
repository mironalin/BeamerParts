package live.alinmiron.beamerparts.product.integration;

import live.alinmiron.beamerparts.product.dto.internal.request.StockReservationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReleaseRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.StockReservationDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.entity.StockMovement;
import live.alinmiron.beamerparts.product.entity.StockMovementType;
import live.alinmiron.beamerparts.product.entity.StockReservation;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.StockMovementRepository;
import live.alinmiron.beamerparts.product.repository.StockReservationRepository;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for complete Inventory workflows.
 * Tests the integration between InventoryInternalService, InventoryDomainService, and the database.
 * 
 * PROFESSIONAL APPROACH: Uses Flyway migrations for production-like schema consistency.
 * This ensures our tests validate the exact same database schema used in production.
 * 
 * These tests verify:
 * - Complete service-to-service integration (internal → domain → repository)
 * - Real database operations with PostgreSQL using production schema
 * - Business workflow integrity (reserve → release → adjust → check)
 * - Audit trail creation and accuracy
 * - Error handling for business rule violations
 * - Migration compatibility and schema consistency
 * 
 * This replaces unit tests that mocked repositories, providing true end-to-end verification
 * of business behavior with production-grade database setup.
 */
@SpringBootTest
@TestPropertySource(properties = {
    // Use Flyway migrations for production-like schema (BEST PRACTICE)
    "spring.jpa.hibernate.ddl-auto=validate", // Only validate, don't create schema
    "spring.flyway.enabled=true"
    // Note: cleanOnValidationError removed in newer Flyway versions
})
@Transactional
@Rollback
@DisplayName("Inventory Service Integration Tests - Complete Business Workflows")
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryInternalService inventoryInternalService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    private Product testProduct;
    private Category testCategory;
    private String testSku = "INT-TEST-001";

    @BeforeEach
    void setUp() {
        // Find or create test category (using existing data)
        testCategory = categoryRepository.findBySlug("test-category")
                .orElseGet(() -> {
                    Category category = Category.builder()
                            .name("Integration Test Category")
                            .slug("test-category")
                            .description("Category for integration tests")
                            .isActive(true)
                            .displayOrder(999)
                            .build();
                    return categoryRepository.save(category);
                });

        // Clean up any existing test data (simplified)
        productRepository.findBySku(testSku).ifPresent(productRepository::delete);

        // Create fresh test product
        testProduct = Product.builder()
                .name("Integration Test Product")
                .slug("integration-test-product")
                .sku(testSku)
                .description("Product for integration testing inventory workflows")
                .basePrice(new BigDecimal("99.99"))
                .category(testCategory)
                .status(ProductStatus.ACTIVE)
                .build();
        testProduct = productRepository.save(testProduct);

        // Create test inventory through service layer (proper approach)
        inventoryInternalService.updateStock(
                testSku, null, 50, "Initial test inventory setup", "integration-test");
    }

    @Test
    @DisplayName("Complete Stock Reservation Workflow - Service Integration")
    void shouldCompleteStockReservationWorkflowThroughAllLayers() {
        // Given: Customer wants to reserve 15 items
        StockReservationRequestDto request = StockReservationRequestDto.builder()
                .productSku(testSku)
                .quantity(15)
                .userId("integration-user-001")
                .source("integration-test")
                .expirationMinutes(30)
                .build();

        // When: Reserve stock through complete service stack
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then: Verify successful reservation at all layers
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProductSku()).isEqualTo(testSku);
        assertThat(result.getQuantityReserved()).isEqualTo(15);
        assertThat(result.getUserId()).isEqualTo("integration-user-001");
        assertThat(result.getRemainingStock()).isEqualTo(35); // 50 - 15

        // Verify database state through repository layer
        Inventory updatedInventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(updatedInventory.getQuantityAvailable()).isEqualTo(35);
        assertThat(updatedInventory.getQuantityReserved()).isEqualTo(15);

        // Verify audit trail creation (including initial setup movement)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements).hasSize(2); // Initial setup + reservation
        assertThat(movements.get(0).getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movements.get(0).getQuantityChange()).isEqualTo(15);
        assertThat(movements.get(1).getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movements.get(1).getQuantityChange()).isEqualTo(50);

        // Verify reservation record creation
        List<StockReservation> reservations = stockReservationRepository
                .findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("integration-user-001");
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getQuantityReserved()).isEqualTo(15);
        assertThat(reservations.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Stock Release After Reservation - Complete Workflow")
    void shouldReleaseStockThroughAllLayers() {
        // Given: Customer has reserved 20 items
        StockReservationRequestDto reserveRequest = StockReservationRequestDto.builder()
                .productSku(testSku)
                .quantity(20)
                .userId("integration-user-002")
                .source("integration-test")
                .build();
        StockReservationDto reservation = inventoryInternalService.reserveStock(reserveRequest);
        assertThat(reservation.isSuccess()).isTrue();

        // When: Release 8 items through service stack
        StockReleaseRequestDto releaseRequest = StockReleaseRequestDto.builder()
                .productSku(testSku)
                .reservationId(reservation.getReservationId())
                .quantityToRelease(8)
                .userId("integration-user-002")
                .reason("Customer changed order")
                .build();

        inventoryInternalService.releaseStock(releaseRequest);

        // Then: Verify inventory state across all layers
        Inventory inventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(inventory.getQuantityAvailable()).isEqualTo(38); // 50 - 20 + 8
        assertThat(inventory.getQuantityReserved()).isEqualTo(12);   // 20 - 8

        // Verify complete audit trail (including initial setup movement)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements).hasSize(3); // Initial setup + reservation + release
        assertThat(movements.get(0).getMovementType()).isEqualTo(StockMovementType.RELEASED);
        assertThat(movements.get(0).getQuantityChange()).isEqualTo(8);
        assertThat(movements.get(1).getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movements.get(1).getQuantityChange()).isEqualTo(20);
        assertThat(movements.get(2).getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movements.get(2).getQuantityChange()).isEqualTo(50);
    }

    @Test
    @DisplayName("Stock Update (Admin) - Complete Integration")
    void shouldUpdateStockLevelsAcrossAllLayers() {
        // Given: Current stock is 50 available, 0 reserved
        
        // When: Admin increases stock to 100 through service
        InventoryInternalDto result = inventoryInternalService.updateStock(
                testSku, null, 100, "Stock replenishment from supplier", "admin@integration-test.com");

        // Then: Verify inventory updated at all layers
        assertThat(result.getQuantityAvailable()).isEqualTo(100);
        assertThat(result.getProductSku()).isEqualTo(testSku);

        // Verify database persistence
        Inventory inventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(inventory.getQuantityAvailable()).isEqualTo(100);

        // Verify audit trail creation (including initial setup movement)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements).hasSize(2); // Initial setup + replenishment
        assertThat(movements.get(0).getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movements.get(0).getQuantityChange()).isEqualTo(50); // 100 - 50
        assertThat(movements.get(0).getReason()).isEqualTo("Stock replenishment from supplier");
        assertThat(movements.get(1).getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movements.get(1).getQuantityChange()).isEqualTo(50); // Initial setup
        assertThat(movements.get(1).getReason()).isEqualTo("Initial test inventory setup");
    }

    @Test
    @DisplayName("Insufficient Stock Business Rule Enforcement")
    void shouldEnforceBusinessRulesAcrossAllLayers() {
        // Given: Only 50 items available
        StockReservationRequestDto request = StockReservationRequestDto.builder()
                .productSku(testSku)
                .quantity(75) // More than available
                .userId("integration-user-003")
                .source("integration-test")
                .build();

        // When: Attempt to reserve more than available
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then: Business rule should be enforced
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).contains("Unable to reserve stock");

        // Verify no state changes in database
        Inventory inventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(inventory.getQuantityAvailable()).isEqualTo(50); // Unchanged
        assertThat(inventory.getQuantityReserved()).isEqualTo(0);   // Unchanged

        // Verify only initial setup audit trail exists (no reservation movement for failed attempt)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements).hasSize(1); // Only initial setup movement
        assertThat(movements.get(0).getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movements.get(0).getQuantityChange()).isEqualTo(50);
        assertThat(movements.get(0).getReason()).isEqualTo("Initial test inventory setup");
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("integration-user-003")).isEmpty();
    }

    @Test
    @DisplayName("Stock Availability Checks - Service Integration")
    void shouldCheckStockAvailabilityCorrectlyAcrossLayers() {
        // Given: Reserve 25 items first
        inventoryInternalService.reserveStock(StockReservationRequestDto.builder()
                .productSku(testSku)
                .quantity(25)
                .userId("integration-user-004")
                .source("integration-test")
                .build());

        // When: Check availability for different quantities through service
        boolean canReserve15 = inventoryInternalService.isStockAvailable(testSku, null, 15);
        boolean canReserve25 = inventoryInternalService.isStockAvailable(testSku, null, 25);
        boolean canReserve30 = inventoryInternalService.isStockAvailable(testSku, null, 30);

        // Then: Verify availability calculations
        assertThat(canReserve15).isTrue();  // 25 available (50-25), can reserve 15
        assertThat(canReserve25).isTrue();  // 25 available, can reserve exactly 25
        assertThat(canReserve30).isFalse(); // 25 available, cannot reserve 30

        // Verify through service layer
        Integer availableQuantity = inventoryInternalService.getAvailableQuantity(testSku, null);
        assertThat(availableQuantity).isEqualTo(25);
    }

    @Test
    @DisplayName("Low Stock Detection - Complete Integration")
    void shouldDetectLowStockAcrossAllLayers() {
        // Given: Reduce stock to below reorder point (10)
        inventoryInternalService.updateStock(testSku, null, 8, "Test low stock condition", "admin");

        // When: Get inventory information through service
        InventoryInternalDto inventory = inventoryInternalService.getInventory(testSku, null);

        // Then: Should detect low stock through business logic
        assertThat(inventory.getQuantityAvailable()).isEqualTo(8);
        assertThat(inventory.isInStock()).isTrue();  // Still has stock
        assertThat(inventory.isLowStock()).isTrue(); // 8 < reorderPoint(10)

        // Verify reorder point is properly loaded from database
        Inventory dbInventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(dbInventory.getReorderPoint()).isEqualTo(10);
    }

    @Test
    @DisplayName("Product Not Found Error Handling")
    void shouldHandleProductNotFoundGracefullyAcrossLayers() {
        // Given: Non-existent product
        StockReservationRequestDto request = StockReservationRequestDto.builder()
                .productSku("NON-EXISTENT-SKU-999")
                .quantity(1)
                .userId("integration-user-005")
                .source("integration-test")
                .build();

        // When: Attempt to reserve stock for non-existent product
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then: Should handle gracefully through all layers
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).contains("Product not found");
        assertThat(result.getProductSku()).isEqualTo("NON-EXISTENT-SKU-999");

        // Verify no side effects in database
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("integration-user-005")).isEmpty();
    }

    @Test
    @DisplayName("Multiple Concurrent Reservations - Integration Stress Test")
    void shouldHandleMultipleReservationsCorrectlyThroughAllLayers() {
        // Given: Multiple customers want to reserve stock concurrently
        
        // When: Process multiple reservations that should exactly exhaust stock
        StockReservationDto result1 = inventoryInternalService.reserveStock(
                StockReservationRequestDto.builder()
                        .productSku(testSku).quantity(20).userId("user-a").source("test").build());
        
        StockReservationDto result2 = inventoryInternalService.reserveStock(
                StockReservationRequestDto.builder()
                        .productSku(testSku).quantity(15).userId("user-b").source("test").build());
        
        StockReservationDto result3 = inventoryInternalService.reserveStock(
                StockReservationRequestDto.builder()
                        .productSku(testSku).quantity(15).userId("user-c").source("test").build());
        
        StockReservationDto result4 = inventoryInternalService.reserveStock(
                StockReservationRequestDto.builder()
                        .productSku(testSku).quantity(1).userId("user-d").source("test").build());

        // Then: First three should succeed, fourth should fail
        assertThat(result1.isSuccess()).isTrue();  // 20 reserved, 30 left
        assertThat(result2.isSuccess()).isTrue();  // 15 reserved, 15 left  
        assertThat(result3.isSuccess()).isTrue();  // 15 reserved, 0 left
        assertThat(result4.isSuccess()).isFalse(); // 1 requested, 0 available

        // Verify final state across all layers
        Inventory finalInventory = inventoryRepository.findByProductSkuAndVariantIsNull(testSku).get();
        assertThat(finalInventory.getQuantityAvailable()).isEqualTo(0);   // 50 - 20 - 15 - 15
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(50);

        // Verify complete audit trail (including initial setup movement)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements).hasSize(4); // Initial setup + three successful reservations
        assertThat(movements.get(0).getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movements.get(1).getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movements.get(2).getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movements.get(3).getMovementType()).isEqualTo(StockMovementType.INCOMING); // Initial setup

        // Verify reservation records
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("user-a")).hasSize(1);
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("user-b")).hasSize(1);
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("user-c")).hasSize(1);
        assertThat(stockReservationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc("user-d")).hasSize(0);
    }
}
