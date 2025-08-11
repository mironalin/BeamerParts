package live.alinmiron.beamerparts.vehicle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BMW Generation DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwGenerationResponseDto {
    
    private Long id;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private String name;
    private String code;
    private Integer yearStart;
    private Integer yearEnd;
    private String[] bodyCodes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String displayName;
    private String yearRange;
    private Boolean isCurrentGeneration;
    private Integer compatibleProductCount;
    
    // Optional nested data
    private List<VehicleCompatibilityRegistryResponseDto> compatibilityEntries;
}
