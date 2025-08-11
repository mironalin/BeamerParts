package live.alinmiron.beamerparts.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BmwGenerationCache DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwGenerationCacheResponseDto {
    
    private String code;
    private String seriesCode;
    private String name;
    private Integer yearStart;
    private Integer yearEnd;
    private String[] bodyCodes;
    private Boolean isActive;
    private LocalDateTime lastUpdated;
    
    // Computed fields
    private String displayName;
    private String yearRange;
    private Boolean isCurrentGeneration;
    private Integer compatibilityCount;
    
    // Optional nested data
    private BmwSeriesCacheResponseDto series;
    private List<ProductCompatibilityResponseDto> compatibility;
}
