package live.alinmiron.beamerparts.product.service.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkStockCheckRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReservationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReleaseRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.StockReservationDto;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.service.domain.InventoryDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal Inventory Service for service-to-service communication
 * Handles stock operations, reservations, and bulk checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryInternalService {
    
    // Domain service for business logic
    private final InventoryDomainService inventoryDomainService;
    
    // Repositories for data access
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    
    /**
     * Reserve stock for orders/cart operations
     * Delegates to domain service for business logic
     */
    public StockReservationDto reserveStock(StockReservationRequestDto request) {
        log.debug("Reserving stock: {} units of {} for user {}", 
                request.getQuantity(), request.getProductSku(), request.getUserId());
        
        try {
            // Get product entity
            Optional<Product> productOpt = productRepository.findBySku(request.getProductSku());
            if (productOpt.isEmpty()) {
                return StockReservationDto.failure(request.getProductSku(), request.getVariantSku(),
                        request.getUserId(), "Product not found");
            }
            
            Product product = productOpt.get();
            
            // Delegate to domain service for business logic
            boolean success = inventoryDomainService.reserveStock(
                    product, request.getVariantSku(), request.getQuantity(), request.getUserId());
            
            if (success) {
                // Get updated inventory for response
                Optional<Inventory> inventoryOpt = inventoryDomainService.getInventory(
                        product, request.getVariantSku());
                
                if (inventoryOpt.isPresent()) {
                    Inventory inventory = inventoryOpt.get();
                    
                    // Calculate expiration time
                    int expirationMinutes = request.getExpirationMinutes() != null ? 
                            request.getExpirationMinutes() : 30;
                    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
                    
                    String reservationId = UUID.randomUUID().toString();
                    
                    return StockReservationDto.success(reservationId, request.getProductSku(), 
                            request.getVariantSku(), request.getQuantity(), request.getUserId(),
                            inventory.getQuantityAvailable(), expiresAt);
                }
            }
            
            // Reservation failed
            return StockReservationDto.failure(request.getProductSku(), request.getVariantSku(),
                    request.getUserId(), "Unable to reserve stock");
            
        } catch (Exception e) {
            log.error("Error reserving stock for {}: {}", request.getProductSku(), e.getMessage());
            return StockReservationDto.failure(request.getProductSku(), request.getVariantSku(),
                    request.getUserId(), "Error processing reservation: " + e.getMessage());
        }
    }
    
    /**
     * Release previously reserved stock
     * Delegates to domain service for business logic
     */
    public void releaseStock(StockReleaseRequestDto request) {
        log.debug("Releasing stock for reservation: {} for user {}", 
                request.getReservationId(), request.getUserId());
        
        // Get product entity
        Optional<Product> productOpt = productRepository.findBySku(request.getProductSku());
        if (productOpt.isEmpty()) {
            log.warn("Product not found for SKU: {}", request.getProductSku());
            return;
        }
        
        Product product = productOpt.get();
        
        // Determine quantity to release
        Integer releaseQuantity = request.getQuantityToRelease();
        if (releaseQuantity == null) {
            // If no quantity specified, release all available reserved stock
            Optional<Inventory> inventoryOpt = inventoryDomainService.getInventory(product, request.getVariantSku());
            releaseQuantity = inventoryOpt.map(Inventory::getQuantityReserved).orElse(0);
        }
        
        // Delegate to domain service for business logic
        String reason = request.getReason() != null ? request.getReason() : "Stock released for reservation " + request.getReservationId();
        inventoryDomainService.releaseStock(product, request.getVariantSku(), releaseQuantity, reason);
    }
    
    /**
     * Bulk stock check for multiple items
     */
    @Transactional(readOnly = true)
    public List<InventoryInternalDto> bulkStockCheck(BulkStockCheckRequestDto request) {
        log.debug("Bulk stock check for {} items", request.getItems().size());
        
        return request.getItems().stream()
                .map(item -> {
                    Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSkuAndVariantSku(
                            item.getSku(), item.getVariantSku());
                    
                    if (inventoryOpt.isPresent()) {
                        return mapToInternalDto(inventoryOpt.get());
                    } else {
                        // Return empty inventory DTO
                        return InventoryInternalDto.builder()
                                .productSku(item.getSku())
                                .variantSkuSuffix(item.getVariantSku())
                                .quantityAvailable(0)
                                .quantityReserved(0)
                                .isInStock(false)
                                .isLowStock(true)
                                .isBelowMinimum(true)
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get inventory for specific product/variant
     * Delegates to domain service
     */
    @Transactional(readOnly = true)
    public InventoryInternalDto getInventory(String sku, String variantSku) {
        log.debug("Getting inventory for SKU: {} variant: {}", sku, variantSku);
        
        Optional<Product> productOpt = productRepository.findBySku(sku);
        if (productOpt.isEmpty()) {
            return InventoryInternalDto.builder()
                    .productSku(sku)
                    .variantSkuSuffix(variantSku)
                    .quantityAvailable(0)
                    .quantityReserved(0)
                    .isInStock(false)
                    .build();
        }
        
        return inventoryDomainService.getInventory(productOpt.get(), variantSku)
                .map(this::mapToInternalDto)
                .orElse(InventoryInternalDto.builder()
                        .productSku(sku)
                        .variantSkuSuffix(variantSku)
                        .quantityAvailable(0)
                        .quantityReserved(0)
                        .isInStock(false)
                        .build());
    }
    
    /**
     * Update stock levels (admin operations)
     * Delegates to domain service for business logic
     */
    public InventoryInternalDto updateStock(String sku, String variantSku, Integer quantity, 
                                           String reason, String userCode) {
        log.debug("Updating stock for SKU: {} to quantity: {}", sku, quantity);
        
        // Get product entity
        Optional<Product> productOpt = productRepository.findBySku(sku);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found for SKU: " + sku);
        }
        
        Product product = productOpt.get();
        
        // Delegate to domain service for business logic
        inventoryDomainService.adjustStock(product, variantSku, quantity, reason);
        
        // Force flush to ensure inventory is persisted before retrieval
        inventoryRepository.flush();
        
        // Get updated inventory for response (may be newly created)
        Optional<Inventory> inventoryOpt = inventoryDomainService.getInventory(product, variantSku);
        if (inventoryOpt.isPresent()) {
            return mapToInternalDto(inventoryOpt.get());
        }
        
        // If inventory still doesn't exist after adjustment, return a default response
        log.warn("Inventory not found after stock adjustment for SKU: {}", sku);
        return InventoryInternalDto.builder()
                .productSku(sku)
                .variantSkuSuffix(variantSku)
                .quantityAvailable(quantity != null ? quantity : 0)
                .quantityReserved(0)
                .isInStock(quantity != null && quantity > 0)
                .build();
    }
    
    /**
     * Check if sufficient stock is available for quantity
     * Delegates to domain service
     */
    @Transactional(readOnly = true)
    public boolean isStockAvailable(String sku, String variantSku, Integer quantity) {
        Optional<Product> productOpt = productRepository.findBySku(sku);
        if (productOpt.isEmpty()) {
            return false;
        }
        return inventoryDomainService.isStockAvailable(productOpt.get(), variantSku, quantity);
    }
    
    /**
     * Check stock availability (used by ProductInternalService)
     */
    @Transactional(readOnly = true)
    public boolean checkStockAvailability(String sku, String variantSku, Integer quantity) {
        return isStockAvailable(sku, variantSku, quantity);
    }
    
    /**
     * Get available quantity (used by ProductInternalService)
     * Delegates to domain service
     */
    @Transactional(readOnly = true)
    public Integer getAvailableQuantity(String sku, String variantSku) {
        Optional<Product> productOpt = productRepository.findBySku(sku);
        if (productOpt.isEmpty()) {
            return 0;
        }
        return inventoryDomainService.getAvailableQuantity(productOpt.get(), variantSku);
    }


    
    /**
     * Map Inventory entity to Internal DTO
     */
    private InventoryInternalDto mapToInternalDto(Inventory inventory) {
        return InventoryInternalDto.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .variantId(inventory.getVariant() != null ? inventory.getVariant().getId() : null)
                .productSku(inventory.getProduct().getSku())
                .variantSkuSuffix(inventory.getVariant() != null ? inventory.getVariant().getSkuSuffix() : null)
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityReserved(inventory.getQuantityReserved())
                .minimumStockLevel(inventory.getMinimumStockLevel())
                .reorderPoint(inventory.getReorderPoint())
                .totalQuantity(inventory.getQuantityAvailable() + inventory.getQuantityReserved())
                .isInStock(inventory.getQuantityAvailable() > 0)
                .isLowStock(inventory.getQuantityAvailable() <= inventory.getReorderPoint())
                .isBelowMinimum(inventory.getQuantityAvailable() < inventory.getMinimumStockLevel())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }
}
