package live.alinmiron.beamerparts.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * User vehicle entity mapping to user_vehicles table
 * User's BMW vehicle preferences (uses codes, not foreign keys)
 */
@Entity
@Table(name = "user_vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserVehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "series_code", nullable = false, length = 10)
    private String seriesCode; // '3', 'X5', etc.
    
    @Column(name = "generation_code", nullable = false, length = 20)
    private String generationCode; // 'F30', 'E90', etc.
    
    @Column(name = "year")
    private Integer year;
    
    @Column(name = "model_variant", length = 100)
    private String modelVariant; // '320i', '330d', etc.
    
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Helper methods
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (year != null) {
            sb.append(year).append(" ");
        }
        sb.append("BMW ").append(seriesCode).append(" Series");
        if (modelVariant != null) {
            sb.append(" ").append(modelVariant);
        }
        sb.append(" (").append(generationCode).append(")");
        return sb.toString();
    }
    
    public boolean isCurrentGeneration() {
        // This could be enhanced to check against actual current generation data
        return year != null && year >= 2019;
    }
}
