package live.alinmiron.beamerparts.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BmwSeriesCache DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwSeriesCacheResponseDto {
    
    private String code;
    private String name;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime lastUpdated;
    
    // Computed fields
    private String displayName;
    private Boolean hasActiveGenerations;
    private Integer generationCount;
    
    // Optional nested data
    private List<BmwGenerationCacheResponseDto> generations;
}
