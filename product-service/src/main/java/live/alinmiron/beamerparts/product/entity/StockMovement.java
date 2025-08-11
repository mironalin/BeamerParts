package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * StockMovement entity mapping to stock_movements table
 * Stock movement tracking and audit trail
 */
@Entity
@Table(name = "stock_movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockMovement {
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;
    
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;
    
    @Column(name = "reason", length = 100)
    private String reason;
    
    @Column(name = "reference_id", length = 50)
    private String referenceId; // Order number, adjustment ID, etc.
    
    @Column(name = "user_code", length = 255)
    private String userCode; // User email or identifier
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Helper methods
    public boolean isIncoming() {
        return StockMovementType.INCOMING.equals(movementType);
    }
    
    public boolean isOutgoing() {
        return StockMovementType.OUTGOING.equals(movementType);
    }
    
    public boolean isAdjustment() {
        return StockMovementType.ADJUSTMENT.equals(movementType);
    }
    
    public boolean isReservation() {
        return StockMovementType.RESERVED.equals(movementType);
    }
    
    public boolean isRelease() {
        return StockMovementType.RELEASED.equals(movementType);
    }
    
    public String getDisplayName() {
        StringBuilder display = new StringBuilder();
        display.append(movementType.name());
        
        if (variant != null) {
            display.append(" - ").append(product.getName()).append(" - ").append(variant.getName());
        } else {
            display.append(" - ").append(product.getName());
        }
        
        display.append(" (").append(quantityChange > 0 ? "+" : "").append(quantityChange).append(")");
        
        return display.toString();
    }
}
