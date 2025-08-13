package live.alinmiron.beamerparts.product.service.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkStockCheckRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReservationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReleaseRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.StockReservationDto;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.service.domain.InventoryDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Comprehensive tests for InventoryInternalService covering edge cases, error handling, and complex business scenarios.
 * Tests delegation to domain service, error handling, and DTO mapping functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryInternalService Tests")
class InventoryInternalServiceTest {

    @Mock
    private InventoryDomainService inventoryDomainService;
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private InventoryInternalService inventoryInternalService;

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
    @DisplayName("reserveStock() should successfully reserve stock when product exists and stock available")
    void reserveStock_WithValidRequest_ShouldSucceedAndReturnSuccess() {
        // Given
        StockReservationRequestDto request = createReservationRequest("TEST-PRODUCT-" + testIdCounter, null, 5, "user123", 30);
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.reserveStock(testProduct, null, 5, "user123"))
                .thenReturn(true);
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.of(testInventory));

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProductSku()).isEqualTo("TEST-PRODUCT-" + testIdCounter);
        assertThat(result.getQuantityReserved()).isEqualTo(5);
        assertThat(result.getUserId()).isEqualTo("user123");
        assertThat(result.getRemainingStock()).isEqualTo(100);
        assertThat(result.getReservationId()).isNotNull();
        assertThat(result.getExpiresAt()).isNotNull();
        
        verify(inventoryDomainService).reserveStock(testProduct, null, 5, "user123");
    }

    @Test
    @DisplayName("reserveStock() should fail when product not found")
    void reserveStock_WithNonExistentProduct_ShouldReturnFailure() {
        // Given
        StockReservationRequestDto request = createReservationRequest("NON-EXISTENT", null, 5, "user123", 30);
        
        when(productRepository.findBySku("NON-EXISTENT"))
                .thenReturn(Optional.empty());

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Product not found");
        assertThat(result.getProductSku()).isEqualTo("NON-EXISTENT");
        
        verifyNoInteractions(inventoryDomainService);
    }

    @Test
    @DisplayName("reserveStock() should fail when domain service returns false")
    void reserveStock_WhenDomainServiceReturnsFalse_ShouldReturnFailure() {
        // Given
        StockReservationRequestDto request = createReservationRequest("TEST-PRODUCT-" + testIdCounter, null, 5, "user123", 30);
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.reserveStock(testProduct, null, 5, "user123"))
                .thenReturn(false);

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Unable to reserve stock");
        
        verify(inventoryDomainService).reserveStock(testProduct, null, 5, "user123");
    }

    @Test
    @DisplayName("reserveStock() should handle exceptions gracefully")
    void reserveStock_WhenExceptionThrown_ShouldReturnFailureWithError() {
        // Given
        StockReservationRequestDto request = createReservationRequest("TEST-PRODUCT-" + testIdCounter, null, 5, "user123", 30);
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.reserveStock(testProduct, null, 5, "user123"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).startsWith("Error processing reservation:");
        assertThat(result.getFailureReason()).contains("Database connection failed");
    }

    @Test
    @DisplayName("reserveStock() should use default expiration when not specified")
    void reserveStock_WithNullExpiration_ShouldUseDefault() {
        // Given
        StockReservationRequestDto request = createReservationRequest("TEST-PRODUCT-" + testIdCounter, null, 5, "user123", null);
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.reserveStock(testProduct, null, 5, "user123"))
                .thenReturn(true);
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.of(testInventory));

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
        assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(31));
    }

    @Test
    @DisplayName("reserveStock() should handle variant products correctly")
    void reserveStock_WithVariantProduct_ShouldPassVariantSku() {
        // Given
        StockReservationRequestDto request = createReservationRequest("TEST-PRODUCT-" + testIdCounter, "VARIANT-XL", 3, "user123", 30);
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.reserveStock(testProduct, "VARIANT-XL", 3, "user123"))
                .thenReturn(true);
        when(inventoryDomainService.getInventory(testProduct, "VARIANT-XL"))
                .thenReturn(Optional.of(testInventory));

        // When
        StockReservationDto result = inventoryInternalService.reserveStock(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getVariantSku()).isEqualTo("VARIANT-XL");
        
        verify(inventoryDomainService).reserveStock(testProduct, "VARIANT-XL", 3, "user123");
        verify(inventoryDomainService).getInventory(testProduct, "VARIANT-XL");
    }

    // =================== Stock Release Tests ===================

    @Test
    @DisplayName("releaseStock() should successfully release stock when product exists")
    void releaseStock_WithValidRequest_ShouldSucceed() {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .reservationId("res-123")
                .productSku("TEST-PRODUCT-" + testIdCounter)
                .variantSku(null)
                .quantityToRelease(5)
                .userId("user123")
                .reason("Order cancelled")
                .build();
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));

        // When
        inventoryInternalService.releaseStock(request);

        // Then
        verify(inventoryDomainService).releaseStock(testProduct, null, 5, "Order cancelled");
    }

    @Test
    @DisplayName("releaseStock() should handle non-existent product gracefully")
    void releaseStock_WithNonExistentProduct_ShouldHandleGracefully() {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .reservationId("res-123")
                .productSku("NON-EXISTENT")
                .quantityToRelease(5)
                .userId("user123")
                .build();
        
        when(productRepository.findBySku("NON-EXISTENT"))
                .thenReturn(Optional.empty());

        // When & Then - Should not throw exception
        assertThatCode(() -> inventoryInternalService.releaseStock(request))
                .doesNotThrowAnyException();
        
        verifyNoInteractions(inventoryDomainService);
    }

    @Test
    @DisplayName("releaseStock() should use reserved quantity when quantity not specified")
    void releaseStock_WithNullQuantity_ShouldUseReservedQuantity() {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .reservationId("res-123")
                .productSku("TEST-PRODUCT-" + testIdCounter)
                .variantSku(null)
                .quantityToRelease(null) // No quantity specified
                .userId("user123")
                .reason("Order cancelled")
                .build();
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.of(testInventory));

        // When
        inventoryInternalService.releaseStock(request);

        // Then
        verify(inventoryDomainService).releaseStock(testProduct, null, 10, "Order cancelled"); // testInventory has 10 reserved
    }

    @Test
    @DisplayName("releaseStock() should use default reason when not provided")
    void releaseStock_WithNullReason_ShouldUseDefaultReason() {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .reservationId("res-123")
                .productSku("TEST-PRODUCT-" + testIdCounter)
                .quantityToRelease(5)
                .userId("user123")
                .reason(null) // No reason provided
                .build();
        
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));

        // When
        inventoryInternalService.releaseStock(request);

        // Then
        verify(inventoryDomainService).releaseStock(testProduct, null, 5, "Stock released for reservation res-123");
    }

    // =================== Bulk Stock Check Tests ===================

    @Test
    @DisplayName("bulkStockCheck() should return inventory for existing products")
    void bulkStockCheck_WithExistingProducts_ShouldReturnInventoryInfo() {
        // Given
        String testSku1 = "TEST-PRODUCT-1-" + testIdCounter;
        String testSku2 = "TEST-PRODUCT-2-" + testIdCounter;
        
        BulkStockCheckRequestDto.StockCheckItemDto item1 = BulkStockCheckRequestDto.StockCheckItemDto.builder()
                .sku(testSku1)
                .variantSku(null)
                .build();
        BulkStockCheckRequestDto.StockCheckItemDto item2 = BulkStockCheckRequestDto.StockCheckItemDto.builder()
                .sku(testSku2)
                .variantSku("VARIANT-XL")
                .build();
        
        BulkStockCheckRequestDto request = BulkStockCheckRequestDto.builder()
                .items(Arrays.asList(item1, item2))
                .build();
        
        Inventory inventory1 = createTestInventory(testProduct, 50, 5, 15, 3);
        inventory1.setId(1L);
        Inventory inventory2 = createTestInventory(testProduct, 25, 0, 10, 2);
        inventory2.setId(2L);
        
        when(inventoryRepository.findByProductSkuAndVariantSku(eq(testSku1), isNull()))
                .thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByProductSkuAndVariantSku(eq(testSku2), eq("VARIANT-XL")))
                .thenReturn(Optional.of(inventory2));

        // When
        List<InventoryInternalDto> result = inventoryInternalService.bulkStockCheck(request);

        // Then
        assertThat(result).hasSize(2);
        
        InventoryInternalDto dto1 = result.get(0);
        assertThat(dto1.getProductSku()).startsWith("TEST-PRODUCT-");
        assertThat(dto1.getQuantityAvailable()).isEqualTo(50);
        assertThat(dto1.isInStock()).isTrue();
        
        InventoryInternalDto dto2 = result.get(1);
        assertThat(dto2.getProductSku()).startsWith("TEST-PRODUCT-");
        assertThat(dto2.getQuantityAvailable()).isEqualTo(25);
        assertThat(dto2.isInStock()).isTrue();
    }

    @Test
    @DisplayName("bulkStockCheck() should return empty inventory for non-existent products")
    void bulkStockCheck_WithNonExistentProducts_ShouldReturnEmptyInventory() {
        // Given
        BulkStockCheckRequestDto.StockCheckItemDto item = BulkStockCheckRequestDto.StockCheckItemDto.builder()
                .sku("NON-EXISTENT")
                .variantSku(null)
                .build();
        
        BulkStockCheckRequestDto request = BulkStockCheckRequestDto.builder()
                .items(Arrays.asList(item))
                .build();
        
        when(inventoryRepository.findByProductSkuAndVariantSku("NON-EXISTENT", null))
                .thenReturn(Optional.empty());

        // When
        List<InventoryInternalDto> result = inventoryInternalService.bulkStockCheck(request);

        // Then
        assertThat(result).hasSize(1);
        
        InventoryInternalDto dto = result.get(0);
        assertThat(dto.getProductSku()).isEqualTo("NON-EXISTENT");
        assertThat(dto.getQuantityAvailable()).isEqualTo(0);
        assertThat(dto.getQuantityReserved()).isEqualTo(0);
        assertThat(dto.isInStock()).isFalse();
        assertThat(dto.isLowStock()).isTrue();
        assertThat(dto.isBelowMinimum()).isTrue();
    }

    // =================== Get Inventory Tests ===================

    @Test
    @DisplayName("getInventory() should return inventory DTO when product and inventory exist")
    void getInventory_WithExistingProduct_ShouldReturnInventoryDto() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.of(testInventory));

        // When
        InventoryInternalDto result = inventoryInternalService.getInventory("TEST-PRODUCT-" + testIdCounter, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).startsWith("TEST-PRODUCT-");
        assertThat(result.getQuantityAvailable()).isEqualTo(100);
        assertThat(result.getQuantityReserved()).isEqualTo(10);
        assertThat(result.isInStock()).isTrue();
    }

    @Test
    @DisplayName("getInventory() should return empty inventory when product not found")
    void getInventory_WithNonExistentProduct_ShouldReturnEmptyInventory() {
        // Given
        when(productRepository.findBySku("NON-EXISTENT"))
                .thenReturn(Optional.empty());

        // When
        InventoryInternalDto result = inventoryInternalService.getInventory("NON-EXISTENT", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("NON-EXISTENT");
        assertThat(result.getQuantityAvailable()).isEqualTo(0);
        assertThat(result.getQuantityReserved()).isEqualTo(0);
        assertThat(result.isInStock()).isFalse();
    }

    @Test
    @DisplayName("getInventory() should return empty inventory when inventory not found")
    void getInventory_WithProductButNoInventory_ShouldReturnEmptyInventory() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.empty());

        // When
        InventoryInternalDto result = inventoryInternalService.getInventory("TEST-PRODUCT-" + testIdCounter, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("TEST-PRODUCT-" + testIdCounter);
        assertThat(result.getQuantityAvailable()).isEqualTo(0);
        assertThat(result.getQuantityReserved()).isEqualTo(0);
        assertThat(result.isInStock()).isFalse();
    }

    // =================== Update Stock Tests ===================

    @Test
    @DisplayName("updateStock() should successfully update stock and return updated inventory")
    void updateStock_WithValidRequest_ShouldUpdateAndReturnInventory() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.of(testInventory));

        // When
        InventoryInternalDto result = inventoryInternalService.updateStock(
                "TEST-PRODUCT-" + testIdCounter, null, 150, "Stock replenishment", "admin");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).startsWith("TEST-PRODUCT-");
        
        verify(inventoryDomainService).adjustStock(testProduct, null, 150, "Stock replenishment");
        verify(inventoryRepository).flush();
    }

    @Test
    @DisplayName("updateStock() should throw exception when product not found")
    void updateStock_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findBySku("NON-EXISTENT"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryInternalService.updateStock(
                "NON-EXISTENT", null, 150, "Stock replenishment", "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found for SKU: NON-EXISTENT");
        
        verifyNoInteractions(inventoryDomainService);
    }

    @Test
    @DisplayName("updateStock() should return default DTO when inventory not found after adjustment")
    void updateStock_WhenInventoryNotFoundAfterAdjustment_ShouldReturnDefault() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getInventory(testProduct, null))
                .thenReturn(Optional.empty());

        // When
        InventoryInternalDto result = inventoryInternalService.updateStock(
                "TEST-PRODUCT-" + testIdCounter, null, 150, "Stock replenishment", "admin");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("TEST-PRODUCT-" + testIdCounter);
        assertThat(result.getQuantityAvailable()).isEqualTo(150);
        assertThat(result.getQuantityReserved()).isEqualTo(0);
        assertThat(result.isInStock()).isTrue();
    }

    // =================== Stock Availability Tests ===================

    @Test
    @DisplayName("isStockAvailable() should delegate to domain service")
    void isStockAvailable_ShouldDelegateToDomin() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.isStockAvailable(testProduct, null, 50))
                .thenReturn(true);

        // When
        boolean result = inventoryInternalService.isStockAvailable("TEST-PRODUCT-" + testIdCounter, null, 50);

        // Then
        assertThat(result).isTrue();
        verify(inventoryDomainService).isStockAvailable(testProduct, null, 50);
    }

    @Test
    @DisplayName("checkStockAvailability() should delegate to isStockAvailable()")
    void checkStockAvailability_ShouldDelegateToIsStockAvailable() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.isStockAvailable(testProduct, null, 30))
                .thenReturn(false);

        // When
        boolean result = inventoryInternalService.checkStockAvailability("TEST-PRODUCT-" + testIdCounter, null, 30);

        // Then
        assertThat(result).isFalse();
        verify(inventoryDomainService).isStockAvailable(testProduct, null, 30);
    }

    @Test
    @DisplayName("getAvailableQuantity() should delegate to domain service")
    void getAvailableQuantity_ShouldDelegateToDomin() {
        // Given
        when(productRepository.findBySku("TEST-PRODUCT-" + testIdCounter))
                .thenReturn(Optional.of(testProduct));
        when(inventoryDomainService.getAvailableQuantity(testProduct, null))
                .thenReturn(75);

        // When
        Integer result = inventoryInternalService.getAvailableQuantity("TEST-PRODUCT-" + testIdCounter, null);

        // Then
        assertThat(result).isEqualTo(75);
        verify(inventoryDomainService).getAvailableQuantity(testProduct, null);
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
        return Inventory.builder()
                .id(testIdCounter++)
                .product(product)
                .quantityAvailable(available)
                .quantityReserved(reserved)
                .reorderPoint(reorderPoint)
                .minimumStockLevel(minimum)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private StockReservationRequestDto createReservationRequest(String sku, String variantSku, int quantity, String userId, Integer expiration) {
        return StockReservationRequestDto.builder()
                .productSku(sku)
                .variantSku(variantSku)
                .quantity(quantity)
                .userId(userId)
                .expirationMinutes(expiration)
                .build();
    }
}
