package live.alinmiron.beamerparts.product.dto.external.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductImage DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponseDto {
    
    private Long id;
    private Long productId;
    private String imageUrl;
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    
    // Computed fields
    private String displayName;
    private Boolean isDefaultImage;
}
