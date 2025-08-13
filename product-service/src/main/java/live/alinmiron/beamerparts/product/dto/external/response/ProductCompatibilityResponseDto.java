package live.alinmiron.beamerparts.product.dto.external.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductCompatibility DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompatibilityResponseDto {
    
    private Long id;
    private Long productId;
    private String generationCode;
    private String notes;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    
    // Computed fields
    private String displayName;
    private String compatibilityStatus;
    
    // Optional nested data
    private BmwGenerationCacheResponseDto generation;
}
