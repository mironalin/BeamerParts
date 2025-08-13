package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.*;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.StockMovementRepository;
import live.alinmiron.beamerparts.product.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for InventoryDomainService business logic, focusing on edge cases, 
 * error handling, and complex business scenarios. Tests business rules and audit trails.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryDomainService Tests")
class InventoryDomainServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private StockMovementRepository stockMovementRepository;
    
    @Mock
    private StockReservationRepository stockReservationRepository;
    
    @InjectMocks
    private InventoryDomainService inventoryDomainService;

    private Product testProduct;
    private Inventory testInventory;
    private long testIdCounter;

    @BeforeEach
    void setUp() {
        testIdCounter = System.currentTimeMillis();
        testProduct = createTestProduct("TEST-PRODUCT-" + testIdCounter, "Test Product");
        testInventory = createTestInventory(testProduct, 100, 10, 20, 5);
    }

    // =================== Stock Reservation Tests ===================

    @Test
    @DisplayName("reserveStock() should successfully reserve stock when sufficient quantity available")
    void reserveStock_WithSufficientStock_ShouldSucceedAndCreateAuditTrail() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(25)).thenReturn(true);

        // When
        boolean result = inventoryDomainService.reserveStock(testProduct, null, 25, "customer123");

        // Then
        assertThat(result).isTrue();
        
        // Verify inventory was updated
        verify(testInventory).reserveQuantity(25);
        verify(inventoryRepository).save(testInventory);
        
        // Verify reservation record was created
        ArgumentCaptor<StockReservation> reservationCaptor = ArgumentCaptor.forClass(StockReservation.class);
        verify(stockReservationRepository).save(reservationCaptor.capture());
        
        StockReservation reservation = reservationCaptor.getValue();
        assertThat(reservation.getProduct()).isEqualTo(testProduct);
        assertThat(reservation.getQuantityReserved()).isEqualTo(25);
        assertThat(reservation.getUserId()).isEqualTo("customer123");
        assertThat(reservation.getIsActive()).isTrue();
        assertThat(reservation.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
        
        // Verify audit trail was created
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getProduct()).isEqualTo(testProduct);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.RESERVED);
        assertThat(movement.getQuantityChange()).isEqualTo(25);
        assertThat(movement.getReason()).contains("customer123");
    }

    @Test
    @DisplayName("reserveStock() should fail when inventory not found")
    void reserveStock_WithNoInventory_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.empty());

        // When
        boolean result = inventoryDomainService.reserveStock(testProduct, null, 25, "customer123");

        // Then
        assertThat(result).isFalse();
        
        // Verify no reservations or movements were created
        verifyNoInteractions(stockReservationRepository);
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    @DisplayName("reserveStock() should fail when insufficient stock available")
    void reserveStock_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(150)).thenReturn(false);

        // When
        boolean result = inventoryDomainService.reserveStock(testProduct, null, 150, "customer123");

        // Then
        assertThat(result).isFalse();
        
        // Verify inventory was not modified
        verify(testInventory, never()).reserveQuantity(anyInt());
        verify(inventoryRepository, never()).save(any());
        
        // Verify no reservations or movements were created
        verifyNoInteractions(stockReservationRepository);
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    @DisplayName("reserveStock() should handle variant products correctly")
    void reserveStock_WithVariantProduct_ShouldUseVariantQuery() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantSku(testProduct.getSku(), "SIZE-L"))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(10)).thenReturn(true);

        // When
        boolean result = inventoryDomainService.reserveStock(testProduct, "SIZE-L", 10, "customer123");

        // Then
        assertThat(result).isTrue();
        
        verify(inventoryRepository).findByProductSkuAndVariantSku(testProduct.getSku(), "SIZE-L");
        verify(inventoryRepository, never()).findByProductSkuAndVariantIsNull(any());
    }

    // =================== Stock Release Tests ===================

    @Test
    @DisplayName("releaseStock() should successfully release reserved stock")
    void releaseStock_WithValidReservation_ShouldSucceedAndCreateAuditTrail() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityReserved()).thenReturn(15);

        // When
        boolean result = inventoryDomainService.releaseStock(testProduct, null, 10, "Order cancelled");

        // Then
        assertThat(result).isTrue();
        
        // Verify inventory was updated
        verify(testInventory).releaseQuantity(10);
        verify(inventoryRepository).save(testInventory);
        
        // Verify audit trail was created
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getProduct()).isEqualTo(testProduct);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.RELEASED);
        assertThat(movement.getQuantityChange()).isEqualTo(10);
        assertThat(movement.getReason()).isEqualTo("Order cancelled");
    }

    @Test
    @DisplayName("releaseStock() should fail when inventory not found")
    void releaseStock_WithNoInventory_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.empty());

        // When
        boolean result = inventoryDomainService.releaseStock(testProduct, null, 10, "Order cancelled");

        // Then
        assertThat(result).isFalse();
        
        // Verify no audit trail was created
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    @DisplayName("releaseStock() should fail when trying to release more than reserved")
    void releaseStock_WithExcessiveQuantity_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityReserved()).thenReturn(5);

        // When
        boolean result = inventoryDomainService.releaseStock(testProduct, null, 10, "Order cancelled");

        // Then
        assertThat(result).isFalse();
        
        // Verify inventory was not modified
        verify(testInventory, never()).releaseQuantity(anyInt());
        verify(inventoryRepository, never()).save(any());
        
        // Verify no audit trail was created
        verifyNoInteractions(stockMovementRepository);
    }

    // =================== Stock Adjustment Tests ===================

    @Test
    @DisplayName("adjustStock() should update existing inventory and create audit trail")
    void adjustStock_WithExistingInventory_ShouldUpdateAndCreateAuditTrail() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityAvailable()).thenReturn(50);

        // When
        inventoryDomainService.adjustStock(testProduct, null, 75, "Stock replenishment");

        // Then
        // Verify inventory was updated
        verify(testInventory).adjustQuantity(75);
        verify(inventoryRepository).save(testInventory);
        verify(inventoryRepository).flush();
        
        // Verify audit trail was created for incoming stock
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getProduct()).isEqualTo(testProduct);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movement.getQuantityChange()).isEqualTo(25); // 75 - 50 = 25
        assertThat(movement.getReason()).isEqualTo("Stock replenishment");
    }

    @Test
    @DisplayName("adjustStock() should create new inventory when none exists")
    void adjustStock_WithNoExistingInventory_ShouldCreateNewInventory() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.empty());

        // When
        inventoryDomainService.adjustStock(testProduct, null, 100, "Initial stock");

        // Then
        // Verify new inventory was created and saved
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        verify(inventoryRepository).flush();
        
        Inventory newInventory = inventoryCaptor.getValue();
        assertThat(newInventory.getProduct()).isEqualTo(testProduct);
        assertThat(newInventory.getReorderPoint()).isEqualTo(10); // Default business rule
        
        // Verify audit trail was created
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(movement.getQuantityChange()).isEqualTo(100);
    }

    @Test
    @DisplayName("adjustStock() should handle stock decrease correctly")
    void adjustStock_WithStockDecrease_ShouldCreateOutgoingMovement() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityAvailable()).thenReturn(100);

        // When
        inventoryDomainService.adjustStock(testProduct, null, 60, "Damaged goods removal");

        // Then
        // Verify audit trail was created for outgoing stock
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.OUTGOING);
        assertThat(movement.getQuantityChange()).isEqualTo(40); // |60 - 100| = 40
        assertThat(movement.getReason()).isEqualTo("Damaged goods removal");
    }

    // =================== Stock Availability Tests ===================

    @Test
    @DisplayName("isStockAvailable() should return true when sufficient stock exists")
    void isStockAvailable_WithSufficientStock_ShouldReturnTrue() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(25)).thenReturn(true);

        // When
        boolean result = inventoryDomainService.isStockAvailable(testProduct, null, 25);

        // Then
        assertThat(result).isTrue();
        verify(testInventory).canReserve(25);
    }

    @Test
    @DisplayName("isStockAvailable() should return false when inventory not found")
    void isStockAvailable_WithNoInventory_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.empty());

        // When
        boolean result = inventoryDomainService.isStockAvailable(testProduct, null, 25);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isStockAvailable() should return false when insufficient stock")
    void isStockAvailable_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(150)).thenReturn(false);

        // When
        boolean result = inventoryDomainService.isStockAvailable(testProduct, null, 150);

        // Then
        assertThat(result).isFalse();
        verify(testInventory).canReserve(150);
    }

    // =================== Get Inventory Tests ===================

    @Test
    @DisplayName("getInventory() should return inventory when found")
    void getInventory_WithExistingInventory_ShouldReturnInventory() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));

        // When
        Optional<Inventory> result = inventoryDomainService.getInventory(testProduct, null);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testInventory);
    }

    @Test
    @DisplayName("getInventory() should handle variant products correctly")
    void getInventory_WithVariantProduct_ShouldUseVariantQuery() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantSku(testProduct.getSku(), "SIZE-M"))
                .thenReturn(Optional.of(testInventory));

        // When
        Optional<Inventory> result = inventoryDomainService.getInventory(testProduct, "SIZE-M");

        // Then
        assertThat(result).isPresent();
        verify(inventoryRepository).findByProductSkuAndVariantSku(testProduct.getSku(), "SIZE-M");
        verify(inventoryRepository, never()).findByProductSkuAndVariantIsNull(any());
    }

    @Test
    @DisplayName("getAvailableQuantity() should return quantity when inventory exists")
    void getAvailableQuantity_WithExistingInventory_ShouldReturnQuantity() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityAvailable()).thenReturn(85);

        // When
        Integer result = inventoryDomainService.getAvailableQuantity(testProduct, null);

        // Then
        assertThat(result).isEqualTo(85);
    }

    @Test
    @DisplayName("getAvailableQuantity() should return zero when inventory not found")
    void getAvailableQuantity_WithNoInventory_ShouldReturnZero() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.empty());

        // When
        Integer result = inventoryDomainService.getAvailableQuantity(testProduct, null);

        // Then
        assertThat(result).isEqualTo(0);
    }

    // =================== Cleanup Expired Reservations Tests ===================

    @Test
    @DisplayName("cleanupExpiredReservations() should release stock for expired reservations")
    void cleanupExpiredReservations_WithExpiredReservations_ShouldReleaseStock() {
        // Given
        StockReservation reservation1 = createExpiredReservation(testProduct, 15, "customer1");
        StockReservation reservation2 = createExpiredReservation(testProduct, 8, "customer2");
        
        List<StockReservation> expiredReservations = Arrays.asList(reservation1, reservation2);
        
        when(stockReservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(expiredReservations);
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityReserved()).thenReturn(25);

        // When
        inventoryDomainService.cleanupExpiredReservations();

        // Then
        // Verify stock was released for each reservation
        verify(testInventory, times(2)).releaseQuantity(anyInt());
        verify(inventoryRepository, times(2)).save(testInventory);
        
        // Verify audit trails were created
        verify(stockMovementRepository, times(2)).save(any(StockMovement.class));
        
        // Verify reservations were marked inactive
        assertThat(reservation1.getIsActive()).isFalse();
        assertThat(reservation2.getIsActive()).isFalse();
        verify(stockReservationRepository, times(2)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("cleanupExpiredReservations() should handle empty list gracefully")
    void cleanupExpiredReservations_WithNoExpiredReservations_ShouldHandleGracefully() {
        // Given
        when(stockReservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // When & Then - Should not throw exception
        assertThatCode(() -> inventoryDomainService.cleanupExpiredReservations())
                .doesNotThrowAnyException();
        
        // Verify no inventory operations were performed
        verifyNoInteractions(inventoryRepository);
        verifyNoInteractions(stockMovementRepository);
    }

    // =================== Edge Cases and Error Handling ===================

    @Test
    @DisplayName("Should handle zero quantity adjustments correctly")
    void adjustStock_WithZeroQuantity_ShouldHandleCorrectly() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityAvailable()).thenReturn(50);

        // When
        inventoryDomainService.adjustStock(testProduct, null, 50, "Inventory check");

        // Then
        // Should still save inventory and create movement for zero difference
        verify(testInventory).adjustQuantity(50);
        verify(inventoryRepository).save(testInventory);
        
        // Zero difference should still create audit record (though quantity would be 0)
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should handle negative stock adjustments correctly")
    void adjustStock_WithNegativeStock_ShouldCreateOutgoingMovement() {
        // Given
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.getQuantityAvailable()).thenReturn(20);

        // When
        inventoryDomainService.adjustStock(testProduct, null, 5, "Product recall");

        // Then
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(StockMovementType.OUTGOING);
        assertThat(movement.getQuantityChange()).isEqualTo(15); // |5 - 20| = 15
    }

    @Test
    @DisplayName("Should handle concurrent access scenarios gracefully")
    void reserveStock_WithConcurrentAccess_ShouldHandleGracefully() {
        // Given - Simulate concurrent modification
        when(inventoryRepository.findByProductSkuAndVariantIsNull(testProduct.getSku()))
                .thenReturn(Optional.of(testInventory));
        when(testInventory.canReserve(50)).thenReturn(true);
        doThrow(new RuntimeException("Optimistic locking failure"))
                .when(inventoryRepository).save(testInventory);

        // When & Then - Should propagate exception (business decision)
        assertThatThrownBy(() -> inventoryDomainService.reserveStock(testProduct, null, 50, "customer123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Optimistic locking failure");
    }

    // =================== Helper Methods ===================

    private Product createTestProduct(String sku, String name) {
        return Product.builder()
                .id(testIdCounter++)
                .sku(sku)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Test product description")
                .shortDescription("Short description")
                .basePrice(new BigDecimal("99.99"))
                .brand("Test Brand")
                .weightGrams(500)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private Inventory createTestInventory(Product product, int available, int reserved, int reorderPoint, int minimum) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getId()).thenReturn(testIdCounter++);
        when(inventory.getProduct()).thenReturn(product);
        when(inventory.getQuantityAvailable()).thenReturn(available);
        when(inventory.getQuantityReserved()).thenReturn(reserved);
        when(inventory.getReorderPoint()).thenReturn(reorderPoint);
        when(inventory.getMinimumStockLevel()).thenReturn(minimum);
        when(inventory.getLastUpdated()).thenReturn(LocalDateTime.now());
        return inventory;
    }

    private StockReservation createExpiredReservation(Product product, int quantity, String userId) {
        StockReservation reservation = StockReservation.builder()
                .id(testIdCounter++)
                .reservationId("res-" + testIdCounter)
                .product(product)
                .quantityReserved(quantity)
                .userId(userId)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .isActive(true)
                .build();
        return reservation;
    }
}
