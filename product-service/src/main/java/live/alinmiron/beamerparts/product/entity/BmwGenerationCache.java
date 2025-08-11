package live.alinmiron.beamerparts.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BmwGenerationCache entity mapping to bmw_generations_cache table
 * Cached BMW generation data for fast compatibility queries
 */
@Entity
@Table(name = "bmw_generations_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BmwGenerationCache {
    
    @Id
    @Column(name = "code", length = 20)
    @EqualsAndHashCode.Include
    private String code; // 'F30', 'E90', etc.
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_code", nullable = false)
    private BmwSeriesCache seriesCache;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "year_start", nullable = false)
    private Integer yearStart;
    
    @Column(name = "year_end")
    private Integer yearEnd;
    
    @Column(name = "body_codes", columnDefinition = "text[]")
    private String[] bodyCodes;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    // Relationships
    @OneToMany(mappedBy = "generationCache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCompatibility> compatibilityEntries;
    
    // Helper methods
    public String getDisplayName() {
        return seriesCache.getName() + " " + name + " (" + getYearRange() + ")";
    }
    
    public String getYearRange() {
        if (yearEnd != null) {
            return yearStart + "-" + yearEnd;
        }
        return yearStart + "-present";
    }
    
    public boolean isCurrentGeneration() {
        return yearEnd == null;
    }
    
    public boolean hasBodyCode(String bodyCode) {
        if (bodyCodes == null || bodyCode == null) {
            return false;
        }
        for (String code : bodyCodes) {
            if (bodyCode.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}
