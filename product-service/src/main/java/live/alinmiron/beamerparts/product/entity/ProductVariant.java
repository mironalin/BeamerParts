package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProductVariant entity mapping to product_variants table
 * Size, color, material variations
 */
@Entity
@Table(name = "product_variants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name; // 'Black', 'Carbon Fiber', etc.
    
    @Column(name = "sku_suffix", nullable = false, length = 20)
    private String skuSuffix; // '-BLK', '-CF', etc.
    
    @Column(name = "price_modifier", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventory;
    
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockMovement> stockMovements;
    
    // Helper methods
    public String getFullSku() {
        return product.getSku() + skuSuffix;
    }
    
    public BigDecimal getEffectivePrice() {
        return product.getBasePrice().add(priceModifier);
    }
    
    public String getDisplayName() {
        return product.getName() + " - " + name;
    }
    
    public boolean hasInventory() {
        return inventory != null && !inventory.isEmpty();
    }
}
