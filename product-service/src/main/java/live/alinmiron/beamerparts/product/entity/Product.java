package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product entity mapping to products table
 * Main product catalog
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;
    
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku; // Primary business identifier
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "short_description", length = 500)
    private String shortDescription;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(name = "brand", length = 100)
    private String brand;
    
    @Column(name = "weight_grams")
    private Integer weightGrams;
    
    @Column(name = "dimensions_json", columnDefinition = "JSONB")
    private String dimensionsJson; // {"length": 10, "width": 5, "height": 2}
    
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventory;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockMovement> stockMovements;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCompatibility> compatibility;
    
    // Helper methods
    public boolean isActive() {
        return ProductStatus.ACTIVE.equals(status);
    }
    
    public boolean isInactive() {
        return ProductStatus.INACTIVE.equals(status);
    }
    
    public boolean isDiscontinued() {
        return ProductStatus.DISCONTINUED.equals(status);
    }
    
    public ProductImage getPrimaryImage() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .orElse(images.get(0));
    }
    
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }
    
    public boolean hasCompatibilityData() {
        return compatibility != null && !compatibility.isEmpty();
    }
    
    public String getDisplayName() {
        return brand != null ? brand + " " + name : name;
    }
}
