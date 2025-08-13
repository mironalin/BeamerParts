package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.StockMovement;
import live.alinmiron.beamerparts.product.entity.StockMovementType;
import live.alinmiron.beamerparts.product.entity.StockReservation;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.StockMovementRepository;
import live.alinmiron.beamerparts.product.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service that encapsulates all inventory business logic.
 * 
 * Business Rules:
 * - Stock reservations are atomic and time-limited
 * - All stock changes create audit trail
 * - Cannot reserve more than available quantity
 * - Inventory alerts when stock is low
 * - Concurrent operations maintain consistency
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryDomainService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReservationRepository stockReservationRepository;

    /**
     * Reserve stock for a product variant.
     * Creates reservation record and audit trail.
     * 
     * @param product the product
     * @param variantSku the variant SKU (null for base product)
     * @param quantity quantity to reserve
     * @param customerId customer making reservation
     * @return true if reservation successful
     */
    public boolean reserveStock(Product product, String variantSku, Integer quantity, String customerId) {
        log.debug("Reserving {} units of product {} variant {} for customer {}", 
                 quantity, product.getSku(), variantSku, customerId);

        Optional<Inventory> inventoryOpt;
        if (variantSku == null) {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantIsNull(product.getSku());
        } else {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantSku(product.getSku(), variantSku);
        }
        
        if (inventoryOpt.isEmpty()) {
            log.warn("No inventory found for product {} variant {}", product.getSku(), variantSku);
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        
        // Business rule: Cannot reserve more than available
        if (!inventory.canReserve(quantity)) {
            log.warn("Insufficient stock: requested {} but only {} available for product {} variant {}", 
                    quantity, inventory.getQuantityAvailable(), product.getSku(), variantSku);
            return false;
        }

        // Reserve the stock
        inventory.reserveQuantity(quantity);
        inventoryRepository.save(inventory);

        // Create reservation record
        String reservationId = UUID.randomUUID().toString();
        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .product(product)
                .quantityReserved(quantity)
                .userId(customerId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // Business rule: 30 min expiry
                .isActive(true)
                .build();
        stockReservationRepository.save(reservation);

        // Create audit trail
        createStockMovement(product, StockMovementType.RESERVED, quantity, 
                           "Stock reserved for customer " + customerId);

        log.info("Successfully reserved {} units of product {} variant {} for customer {}", 
                quantity, product.getSku(), variantSku, customerId);
        return true;
    }

    /**
     * Release stock reservation.
     * Updates inventory and creates audit trail.
     */
    public boolean releaseStock(Product product, String variantSku, Integer quantity, String reason) {
        log.debug("Releasing {} units of product {} variant {} - reason: {}", 
                 quantity, product.getSku(), variantSku, reason);

        Optional<Inventory> inventoryOpt;
        if (variantSku == null) {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantIsNull(product.getSku());
        } else {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantSku(product.getSku(), variantSku);
        }
        
        if (inventoryOpt.isEmpty()) {
            log.warn("No inventory found for product {} variant {}", product.getSku(), variantSku);
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        
        // Business rule: Cannot release more than reserved
        if (inventory.getQuantityReserved() < quantity) {
            log.warn("Cannot release {} units - only {} reserved for product {} variant {}", 
                    quantity, inventory.getQuantityReserved(), product.getSku(), variantSku);
            return false;
        }

        // Release the stock using rich domain method
        inventory.releaseQuantity(quantity);
        inventoryRepository.save(inventory);

        // Create audit trail
        createStockMovement(product, StockMovementType.RELEASED, quantity, reason);

        log.info("Successfully released {} units of product {} variant {} - reason: {}", 
                quantity, product.getSku(), variantSku, reason);
        return true;
    }

    /**
     * Adjust stock levels (for inventory management).
     * Creates audit trail for all adjustments.
     */
    public void adjustStock(Product product, String variantSku, Integer newQuantity, String reason) {
        log.debug("Adjusting stock for product {} variant {} to {} - reason: {}", 
                 product.getSku(), variantSku, newQuantity, reason);

        Optional<Inventory> inventoryOpt;
        if (variantSku == null) {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantIsNull(product.getSku());
        } else {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantSku(product.getSku(), variantSku);
        }
        
        Inventory inventory = inventoryOpt.orElseGet(() -> {
            log.info("Creating new inventory record for product {} variant {}", 
                    product.getSku(), variantSku);
            return Inventory.builder()
                    .product(product)
                    .quantityAvailable(0)
                    .quantityReserved(0)
                    .reorderPoint(10) // Default business rule
                    .lastUpdated(LocalDateTime.now())
                    .build();
        });

        Integer oldQuantity = inventory.getQuantityAvailable();
        Integer difference = newQuantity - oldQuantity;

        // Adjust quantity using rich domain method
        inventory.adjustQuantity(newQuantity);
        inventoryRepository.save(inventory);
        
        // Force flush to ensure inventory is persisted immediately
        inventoryRepository.flush();
        
        log.debug("Inventory saved with ID: {} for product {} variant {}", 
                inventory.getId(), product.getSku(), variantSku);

        // Create audit trail
        StockMovementType movementType = difference > 0 ? StockMovementType.INCOMING : StockMovementType.OUTGOING;
        createStockMovement(product, movementType, Math.abs(difference), reason);

        log.info("Adjusted stock for product {} variant {} from {} to {} - reason: {}", 
                product.getSku(), variantSku, oldQuantity, newQuantity, reason);
    }

    /**
     * Check if stock is available for reservation.
     */
    public boolean isStockAvailable(Product product, String variantSku, Integer quantity) {
        Optional<Inventory> inventoryOpt;
        if (variantSku == null) {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantIsNull(product.getSku());
        } else {
            inventoryOpt = inventoryRepository.findByProductSkuAndVariantSku(product.getSku(), variantSku);
        }
        
        return inventoryOpt.map(inventory -> inventory.canReserve(quantity))
                          .orElse(false);
    }

    /**
     * Get current inventory for a product variant.
     */
    public Optional<Inventory> getInventory(Product product, String variantSku) {
        Optional<Inventory> result;
        
        if (variantSku == null) {
            // Use simpler query for null variant case
            result = inventoryRepository.findByProductSkuAndVariantIsNull(product.getSku());
        } else {
            // Use complex query for variant case
            result = inventoryRepository.findByProductSkuAndVariantSku(product.getSku(), variantSku);
        }
        
        log.info("getInventory for product {} variant {}: found={}", 
                product.getSku(), variantSku, result.isPresent());
        return result;
    }

    /**
     * Get available quantity for a product variant.
     */
    public Integer getAvailableQuantity(Product product, String variantSku) {
        return getInventory(product, variantSku)
                .map(Inventory::getQuantityAvailable)
                .orElse(0);
    }

    /**
     * Clean up expired reservations (scheduled task).
     * Business rule: Reservations expire after 30 minutes.
     */
    @Transactional
    public void cleanupExpiredReservations() {
        log.debug("Cleaning up expired stock reservations");
        
        List<StockReservation> expiredReservations = stockReservationRepository
                .findExpiredReservations(LocalDateTime.now());
        
        for (StockReservation reservation : expiredReservations) {
            log.info("Releasing expired reservation: {} units of product {} for customer {}", 
                    reservation.getQuantityReserved(), reservation.getProduct().getSku(), 
                    reservation.getUserId());
            
            // Release the reserved stock
            String variantSku = reservation.getVariant() != null ? reservation.getVariant().getSkuSuffix() : null;
            releaseStock(reservation.getProduct(), variantSku, 
                        reservation.getQuantityReserved(), "Reservation expired");
            
            // Mark reservation as inactive
            reservation.setIsActive(false);
            stockReservationRepository.save(reservation);
        }
        
        log.info("Cleaned up {} expired reservations", expiredReservations.size());
    }

    /**
     * Create stock movement audit record.
     */
    private void createStockMovement(Product product, StockMovementType type, Integer quantity, String reason) {
        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(type)
                .quantityChange(quantity)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();
        
        stockMovementRepository.save(movement);
        log.debug("Created stock movement: {} {} units of product {} - {}", 
                 type, quantity, product.getSku(), reason);
    }
}
