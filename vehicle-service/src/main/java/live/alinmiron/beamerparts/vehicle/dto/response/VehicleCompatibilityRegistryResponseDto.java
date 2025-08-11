package live.alinmiron.beamerparts.vehicle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vehicle Compatibility Registry DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCompatibilityRegistryResponseDto {
    
    private Long id;
    private Long generationId;
    private String generationCode;
    private String generationName;
    private String seriesCode;
    private String seriesName;
    private String productSku;
    private String notes;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String displayName;
    
    // Optional extended info (when fetched with product service data)
    private String productName;
    private String productDescription;
    private Boolean productActive;
}
