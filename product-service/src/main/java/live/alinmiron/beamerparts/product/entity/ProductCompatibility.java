package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ProductCompatibility entity mapping to product_compatibility table
 * BMW compatibility mapping using cached data
 */
@Entity
@Table(name = "product_compatibility")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductCompatibility {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_code", nullable = false)
    private BmwGenerationCache generationCache;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Helper methods
    public String getDisplayName() {
        return product.getName() + " - " + generationCache.getDisplayName();
    }
    
    public boolean isCompatibleWith(String generationCode) {
        return generationCache.getCode().equals(generationCode);
    }
    
    public String getCompatibilityStatus() {
        if (isVerified) {
            return "Verified Compatible";
        }
        return "Compatibility Not Verified";
    }
}
