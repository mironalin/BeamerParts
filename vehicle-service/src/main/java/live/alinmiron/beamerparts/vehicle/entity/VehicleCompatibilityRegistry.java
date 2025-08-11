package live.alinmiron.beamerparts.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Vehicle Compatibility Registry entity mapping to vehicle_compatibility_registry table
 * Compatibility registry (uses product SKUs, not foreign keys)
 */
@Entity
@Table(name = "vehicle_compatibility_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehicleCompatibilityRegistry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private BmwGeneration generation;
    
    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku; // 'BMW-F30-AC-001'
    
    @Column(name = "notes", length = 500)
    private String notes; // 'For all 3 Series models'
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public String getGenerationCode() {
        return generation != null ? generation.getCode() : null;
    }
    
    public String getSeriesCode() {
        return generation != null ? generation.getSeriesCode() : null;
    }
    
    public String getDisplayName() {
        if (generation != null) {
            return productSku + " → " + generation.getDisplayName();
        }
        return productSku;
    }
    
    public boolean isForSeries(String seriesCode) {
        return generation != null && seriesCode.equals(generation.getSeriesCode());
    }
    
    public boolean isForGeneration(String generationCode) {
        return generation != null && generationCode.equals(generation.getCode());
    }
}
