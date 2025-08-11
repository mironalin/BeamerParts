package live.alinmiron.beamerparts.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BMW Series entity mapping to bmw_series table
 * BMW Series master data
 */
@Entity
@Table(name = "bmw_series")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BmwSeries {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "name", nullable = false, length = 50)
    private String name; // '3 Series', 'X5', etc.
    
    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code; // '3', 'X5', etc.
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    
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
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BmwGeneration> generations;
    
    // Helper methods
    public String getDisplayName() {
        return "BMW " + name;
    }
    
    public boolean hasActiveGenerations() {
        return generations != null && generations.stream()
                .anyMatch(BmwGeneration::getIsActive);
    }
}
