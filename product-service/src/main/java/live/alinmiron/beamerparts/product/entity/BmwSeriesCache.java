package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BmwSeriesCache entity mapping to bmw_series_cache table
 * Cached BMW data for fast compatibility queries
 */
@Entity
@Table(name = "bmw_series_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BmwSeriesCache {
    
    @Id
    @Column(name = "code", length = 10)
    @EqualsAndHashCode.Include
    private String code; // '3', 'X5', etc.
    
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    // Relationships
    @OneToMany(mappedBy = "seriesCache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BmwGenerationCache> generations;
    
    // Helper methods
    public String getDisplayName() {
        return "BMW " + name;
    }
    
    public boolean hasActiveGenerations() {
        return generations != null && generations.stream()
                .anyMatch(BmwGenerationCache::getIsActive);
    }
}
