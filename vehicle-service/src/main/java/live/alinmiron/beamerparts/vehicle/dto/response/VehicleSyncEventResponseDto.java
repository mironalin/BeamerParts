package live.alinmiron.beamerparts.vehicle.dto.response;

import live.alinmiron.beamerparts.vehicle.entity.VehicleSyncEventStatus;
import live.alinmiron.beamerparts.vehicle.entity.VehicleSyncEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vehicle Sync Event DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSyncEventResponseDto {
    
    private Long id;
    private VehicleSyncEventType eventType;
    private String entityCode;
    private String eventData;
    private LocalDateTime publishedAt;
    private VehicleSyncEventStatus processingStatus;
    
    // Computed fields
    private String displayName;
    private Boolean isPending;
    private Boolean isProcessed;
    private Boolean isFailed;
}
