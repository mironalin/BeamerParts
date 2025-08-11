package live.alinmiron.beamerparts.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;
import java.util.List;

/**
 * BMW Generation entity mapping to bmw_generations table
 * BMW Generations master data
 */
@Entity
@Table(name = "bmw_generations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BmwGeneration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private BmwSeries series;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name; // 'E46', 'F30/F31/F34/F35', etc.
    
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code; // 'E46', 'F30', etc.
    
    @Column(name = "year_start", nullable = false)
    private Integer yearStart;
    
    @Column(name = "year_end")
    private Integer yearEnd; // NULL for current generation
    
    @Column(name = "body_codes", columnDefinition = "text[]")
    private String[] bodyCodes; // ['F30', 'F31', 'F34', 'F35'] for variants
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleCompatibilityRegistry> compatibilityEntries;
    
    // Helper methods
    public String getDisplayName() {
        return series.getName() + " " + name + " (" + getYearRange() + ")";
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
        if (bodyCodes == null) return false;
        for (String code : bodyCodes) {
            if (code.equals(bodyCode)) return true;
        }
        return false;
    }
    
    public String getSeriesCode() {
        return series != null ? series.getCode() : null;
    }
}
