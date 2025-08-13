package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Inventory entity mapping to inventory table
 * Stock tracking and management
 */
@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
    
    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;
    
    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;
    
    @Column(name = "minimum_stock_level", nullable = false)
    @Builder.Default
    private Integer minimumStockLevel = 5;
    
    @Column(name = "reorder_point", nullable = false)
    @Builder.Default
    private Integer reorderPoint = 10;
    
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    // Helper methods
    public Integer getTotalQuantity() {
        return quantityAvailable + quantityReserved;
    }
    
    public boolean isLowStock() {
        return quantityAvailable <= reorderPoint;
    }
    
    public boolean isOutOfStock() {
        return quantityAvailable == 0;
    }
    
    public boolean isBelowMinimum() {
        return quantityAvailable < minimumStockLevel;
    }
    
    public boolean canReserve(Integer quantity) {
        return quantity != null && quantity > 0 && quantityAvailable >= quantity;
    }
    
    public void makeReservation(Integer quantity) {
        if (canReserve(quantity)) {
            quantityAvailable -= quantity;
            quantityReserved += quantity;
        } else {
            throw new IllegalArgumentException("Cannot reserve " + quantity + " items. Available: " + quantityAvailable);
        }
    }
    
    public void releaseReservation(Integer quantity) {
        if (quantity != null && quantity > 0 && quantityReserved >= quantity) {
            quantityReserved -= quantity;
            quantityAvailable += quantity;
        } else {
            throw new IllegalArgumentException("Cannot release " + quantity + " items. Reserved: " + quantityReserved);
        }
    }
    
    /**
     * Business method: Reserve quantity (used by domain service)
     */
    public void reserveQuantity(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException("Cannot reserve " + quantity + " items. Available: " + quantityAvailable);
        }
        makeReservation(quantity);
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Business method: Release reserved quantity (used by domain service)
     */
    public void releaseQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0 || quantityReserved < quantity) {
            throw new IllegalArgumentException("Cannot release " + quantity + " items. Reserved: " + quantityReserved);
        }
        releaseReservation(quantity);
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Business method: Adjust total available quantity (inventory management)
     */
    public void adjustQuantity(Integer newQuantity) {
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + newQuantity);
        }
        
        // Cannot reduce below current reservations
        if (newQuantity < quantityReserved) {
            throw new IllegalArgumentException("Cannot set quantity to " + newQuantity + 
                " - would conflict with " + quantityReserved + " reserved items");
        }
        
        quantityAvailable = newQuantity - quantityReserved;
        lastUpdated = LocalDateTime.now();
    }
    
    public String getDisplayName() {
        if (variant != null) {
            return product.getName() + " - " + variant.getName();
        }
        return product.getName();
    }
}
