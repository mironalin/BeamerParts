package live.alinmiron.beamerparts.product.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.entity.StockMovement;
import live.alinmiron.beamerparts.product.entity.StockMovementType;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.StockMovementRepository;
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
 * Business specifications for the Inventory Domain
 * 
 * These tests define the EXPECTED BUSINESS BEHAVIOR of inventory management.
 * They serve as living documentation and drive the implementation.
 * 
 * Business Rules Under Test:
 * 1. Stock Reservations must be atomic and traceable
 * 2. Cannot reserve more than available quantity
 * 3. Stock movements provide complete audit trail
 * 4. Inventory alerts when stock is low or below minimum
 * 5. Concurrent operations maintain data consistency
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Use Flyway migrations for production-like schema (BEST PRACTICE)
        "spring.jpa.hibernate.ddl-auto=validate", // Only validate, don't create schema
        "spring.flyway.enabled=true",
        // "spring.flyway.clean-on-validation-error=true" // Clean and retry if schema mismatch
    })
@Transactional
@Rollback
@DisplayName("Inventory Domain Business Specifications")
class InventoryDomainSpecification {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Product testProduct;
    private Inventory testInventory;

    @BeforeEach
    void setupBusinessScenario() {
        // Given: A real business scenario with actual BMW product
        Category category = Category.builder()
                .name("Interior Parts")
                .slug("interior-parts")
                .description("BMW interior accessories and replacement parts")
                .isActive(true)
                .build();
        categoryRepository.save(category);

        testProduct = Product.builder()
                .name("BMW F30 Cup Holder")
                .slug("bmw-f30-cup-holder")
                .sku("BMW-F30-CH-001")
                .description("OEM replacement cup holder for BMW F30 series")
                .basePrice(new BigDecimal("45.99"))
                .category(category)
                .brand("BMW")
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(testProduct);

        testInventory = Inventory.builder()
                .product(testProduct)
                .variant(null) // No variant for this test
                .quantityAvailable(20)
                .quantityReserved(5)
                .minimumStockLevel(3)
                .reorderPoint(8)
                .build();
        inventoryRepository.save(testInventory);
    }

    @Test
    @DisplayName("Should successfully reserve stock when sufficient quantity is available")
    void shouldReserveStock_whenSufficientQuantityAvailable() {
        // Given: Product with 20 available, 5 already reserved
        // When: Customer wants to reserve 10 units
        Integer requestedQuantity = 10;
        String userId = "customer@example.com";
        String reason = "Shopping cart reservation";

        // Execute business operation
        testInventory.reserveQuantity(requestedQuantity);
        inventoryRepository.save(testInventory);

        // Create audit trail (business requirement)
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(null)
                .movementType(StockMovementType.RESERVED)
                .quantityChange(requestedQuantity)
                .reason(reason)
                .referenceId("CART-123")
                .userCode(userId)
                .build();
        stockMovementRepository.save(movement);

        // Then: Business expectations are met
        Inventory updatedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        
        assertThat(updatedInventory.getQuantityAvailable())
                .as("Available quantity should decrease by reserved amount")
                .isEqualTo(10); // 20 - 10 = 10
        
        assertThat(updatedInventory.getQuantityReserved())
                .as("Reserved quantity should increase by reserved amount")
                .isEqualTo(15); // 5 + 10 = 15
        
        assertThat(updatedInventory.getTotalQuantity())
                .as("Total quantity should remain unchanged")
                .isEqualTo(25); // 10 + 15 = 25

        // Audit trail must be created (business requirement)
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements)
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.getMovementType()).isEqualTo(StockMovementType.RESERVED);
                    assertThat(m.getQuantityChange()).isEqualTo(10);
                    assertThat(m.getUserCode()).isEqualTo(userId);
                    assertThat(m.getReferenceId()).isEqualTo("CART-123");
                });
    }

    @Test
    @DisplayName("Should reject stock reservation when insufficient quantity available")
    void shouldRejectReservation_whenInsufficientStock() {
        // Given: Product with 20 available
        // When: Customer attempts to reserve 25 units (more than available)
        Integer requestedQuantity = 25;

        // Then: Business rule violation should be prevented
        assertThatThrownBy(() -> testInventory.reserveQuantity(requestedQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reserve 25 items")
                .hasMessageContaining("Available: 20");

        // And: Inventory should remain unchanged
        Inventory unchangedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertThat(unchangedInventory.getQuantityAvailable()).isEqualTo(20);
        assertThat(unchangedInventory.getQuantityReserved()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should release stock reservation and restore availability")
    void shouldReleaseReservation_andRestoreAvailability() {
        // Given: Product with some stock reserved
        // When: Customer cancels reservation or it expires
        Integer releaseQuantity = 3;
        String reason = "Customer cancelled order";

        testInventory.releaseReservation(releaseQuantity);
        inventoryRepository.save(testInventory);

        // Create audit trail for release
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .movementType(StockMovementType.RELEASED)
                .quantityChange(releaseQuantity)
                .reason(reason)
                .referenceId("ORDER-456")
                .userCode("system")
                .build();
        stockMovementRepository.save(movement);

        // Then: Stock should be returned to available pool
        Inventory updatedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        
        assertThat(updatedInventory.getQuantityAvailable())
                .as("Available quantity should increase by released amount")
                .isEqualTo(23); // 20 + 3 = 23
        
        assertThat(updatedInventory.getQuantityReserved())
                .as("Reserved quantity should decrease by released amount")
                .isEqualTo(2); // 5 - 3 = 2

        // Audit trail must record the release
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements)
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.getMovementType()).isEqualTo(StockMovementType.RELEASED);
                    assertThat(m.getQuantityChange()).isEqualTo(3);
                    assertThat(m.getReason()).isEqualTo(reason);
                });
    }

    @Test
    @DisplayName("Should detect low stock condition based on reorder point")
    void shouldDetectLowStock_whenBelowReorderPoint() {
        // Given: Product with reorder point of 8
        // When: Available stock drops to 8 or below
        testInventory.setQuantityAvailable(8);
        inventoryRepository.save(testInventory);

        // Then: System should detect low stock condition
        Inventory inventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertThat(inventory.isLowStock())
                .as("Should detect low stock when available equals reorder point")
                .isTrue();

        // When: Stock drops below reorder point
        inventory.setQuantityAvailable(6);
        inventoryRepository.save(inventory);

        inventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertThat(inventory.isLowStock())
                .as("Should detect low stock when below reorder point")
                .isTrue();
    }

    @Test
    @DisplayName("Should detect critical stock condition when below minimum level")
    void shouldDetectCriticalStock_whenBelowMinimumLevel() {
        // Given: Product with minimum stock level of 3
        // When: Available stock drops below minimum
        testInventory.setQuantityAvailable(2);
        inventoryRepository.save(testInventory);

        // Then: System should detect critical stock condition
        Inventory inventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertThat(inventory.isBelowMinimum())
                .as("Should detect critical stock when below minimum level")
                .isTrue();
        
        assertThat(inventory.isOutOfStock())
                .as("Should not be out of stock when quantity is 2")
                .isFalse();
    }

    @Test
    @DisplayName("Should handle stock adjustment with proper audit trail")
    void shouldHandleStockAdjustment_withAuditTrail() {
        // Given: Current inventory levels
        Integer oldQuantity = testInventory.getQuantityAvailable();
        
        // When: Admin performs stock adjustment (e.g., after physical count)
        Integer newQuantity = 30;
        Integer adjustment = newQuantity - oldQuantity; // +10
        String reason = "Physical inventory count adjustment";
        String adminUser = "admin@beamerparts.com";

        testInventory.setQuantityAvailable(newQuantity);
        testInventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(testInventory);

        // Create audit trail for adjustment
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .movementType(StockMovementType.ADJUSTMENT)
                .quantityChange(Math.abs(adjustment))
                .reason(reason)
                .referenceId("ADJ-2024-001")
                .userCode(adminUser)
                .build();
        stockMovementRepository.save(movement);

        // Then: Inventory should be updated with audit trail
        Inventory updatedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertThat(updatedInventory.getQuantityAvailable()).isEqualTo(30);
        assertThat(updatedInventory.getLastUpdated()).isNotNull();

        // Audit trail should reflect the adjustment
        List<StockMovement> movements = stockMovementRepository.findByProductOrderByCreatedAtDesc(testProduct);
        assertThat(movements)
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.getMovementType()).isEqualTo(StockMovementType.ADJUSTMENT);
                    assertThat(m.getQuantityChange()).isEqualTo(10);
                    assertThat(m.getReason()).isEqualTo(reason);
                    assertThat(m.getUserCode()).isEqualTo(adminUser);
                    assertThat(m.getCreatedAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("Should prevent invalid stock operations")
    void shouldPreventInvalidStockOperations() {
        // Cannot reserve negative or zero quantities
        assertThatThrownBy(() -> testInventory.reserveQuantity(0))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> testInventory.reserveQuantity(-5))
                .isInstanceOf(IllegalArgumentException.class);

        // Cannot release more than reserved
        assertThatThrownBy(() -> testInventory.releaseReservation(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release 10 items")
                .hasMessageContaining("Reserved: 5");

        // Cannot release negative quantities
        assertThatThrownBy(() -> testInventory.releaseReservation(-3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should maintain business invariants during stock operations")
    void shouldMaintainBusinessInvariants() {
        // Business Invariant: Total quantity (available + reserved) should not change during reservations
        Integer initialTotal = testInventory.getTotalQuantity();
        
        // Reserve some stock
        testInventory.reserveQuantity(5);
        assertThat(testInventory.getTotalQuantity())
                .as("Total quantity should remain constant during reservation")
                .isEqualTo(initialTotal);

        // Release some stock
        testInventory.releaseReservation(3);
        assertThat(testInventory.getTotalQuantity())
                .as("Total quantity should remain constant during release")
                .isEqualTo(initialTotal);

        // Business Invariant: Available and reserved quantities must be non-negative
        assertThat(testInventory.getQuantityAvailable())
                .as("Available quantity must be non-negative")
                .isGreaterThanOrEqualTo(0);
        
        assertThat(testInventory.getQuantityReserved())
                .as("Reserved quantity must be non-negative")
                .isGreaterThanOrEqualTo(0);
    }
}
