package live.alinmiron.beamerparts.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product image creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductImageRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
    
    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;
    
    @Builder.Default
    private Boolean isPrimary = false;
    
    @NotNull(message = "Sort order is required")
    @PositiveOrZero(message = "Sort order must be zero or positive")
    private Integer sortOrder;
}
