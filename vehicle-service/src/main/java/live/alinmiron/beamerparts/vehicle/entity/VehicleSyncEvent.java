package live.alinmiron.beamerparts.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Vehicle Sync Event entity mapping to vehicle_sync_events table
 * Event publishing log for synchronization
 */
@Entity
@Table(name = "vehicle_sync_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehicleSyncEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private VehicleSyncEventType eventType; // 'SERIES_UPDATED', 'GENERATION_UPDATED'
    
    @Column(name = "entity_code", nullable = false, length = 50)
    private String entityCode; // Series or generation code
    
    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData; // Full entity data as JSON
    
    @CreationTimestamp
    @Column(name = "published_at", nullable = false, updatable = false)
    private LocalDateTime publishedAt;
    
    @Column(name = "processing_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VehicleSyncEventStatus processingStatus = VehicleSyncEventStatus.PENDING;
    
    // Helper methods
    public boolean isPending() {
        return VehicleSyncEventStatus.PENDING.equals(processingStatus);
    }
    
    public boolean isProcessed() {
        return VehicleSyncEventStatus.PROCESSED.equals(processingStatus);
    }
    
    public boolean isFailed() {
        return VehicleSyncEventStatus.FAILED.equals(processingStatus);
    }
    
    public void markAsProcessed() {
        this.processingStatus = VehicleSyncEventStatus.PROCESSED;
    }
    
    public void markAsFailed() {
        this.processingStatus = VehicleSyncEventStatus.FAILED;
    }
    
    public String getDisplayName() {
        return eventType + " - " + entityCode + " (" + processingStatus + ")";
    }
}
