package live.alinmiron.beamerparts.vehicle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BMW Series DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwSeriesResponseDto {
    
    private Long id;
    private String name;
    private String code;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String displayName;
    private Boolean hasActiveGenerations;
    private Integer generationCount;
    
    // Optional nested data
    private List<BmwGenerationResponseDto> generations;
}
