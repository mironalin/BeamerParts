package live.alinmiron.beamerparts.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cart item entity mapping to cart_items table
 * Shopping cart functionality (uses product SKUs, not foreign keys)
 */
@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku; // 'BMW-F30-AC-001'
    
    @Column(name = "variant_sku", length = 20)
    private String variantSku; // '-BLK', '-CF', etc.
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Price snapshot
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public String getFullSku() {
        return variantSku != null ? productSku + variantSku : productSku;
    }
    
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public boolean isValidQuantity() {
        return quantity != null && quantity > 0 && quantity <= 99;
    }
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 1 || newQuantity > 99) {
            throw new IllegalArgumentException("Quantity must be between 1 and 99");
        }
        this.quantity = newQuantity;
    }
    
    public void incrementQuantity() {
        updateQuantity(this.quantity + 1);
    }
    
    public void decrementQuantity() {
        if (this.quantity > 1) {
            updateQuantity(this.quantity - 1);
        }
    }
}
