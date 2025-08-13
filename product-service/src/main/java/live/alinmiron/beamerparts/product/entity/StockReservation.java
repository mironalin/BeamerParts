package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stock reservation entity for tracking temporary stock holds
 */
@Entity
@Table(name = "stock_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reservation_id", nullable = false, unique = true, length = 50)
    private String reservationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
    
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;
    
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;
    
    @Column(name = "order_id", length = 50)
    private String orderId;
    
    @Column(name = "source", length = 20)
    private String source; // cart, order, admin
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
