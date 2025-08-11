package live.alinmiron.beamerparts.vehicle.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Add product compatibility request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductCompatibilityRequestDto {
    
    @NotBlank(message = "Generation code is required")
    @Size(max = 20, message = "Generation code must be less than 20 characters")
    private String generationCode;
    
    @NotBlank(message = "Product SKU is required")
    @Size(max = 50, message = "Product SKU must be less than 50 characters")
    private String productSku;
    
    @Size(max = 500, message = "Notes must be less than 500 characters")
    private String notes;
    
    @Builder.Default
    private Boolean isVerified = false;
}
