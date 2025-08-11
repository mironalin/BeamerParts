package live.alinmiron.beamerparts.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product compatibility creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductCompatibilityRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Generation code is required")
    @Size(max = 20, message = "Generation code must not exceed 20 characters")
    private String generationCode;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
    
    @Builder.Default
    private Boolean isVerified = false;
}
