package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Internal request DTO for bulk product operations
 * Used by User Service and other services to get multiple products at once
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkProductRequestDto {
    
    @NotEmpty(message = "SKU list cannot be empty")
    @Size(max = 100, message = "Cannot request more than 100 products at once")
    private List<String> skus;
    
    // Optional flags to include related data
    @Builder.Default
    private Boolean includeInventory = false;
    
    @Builder.Default
    private Boolean includeVariants = false;
    
    @Builder.Default
    private Boolean includeCompatibility = false;
    
    @Builder.Default
    private Boolean includeImages = false;
}