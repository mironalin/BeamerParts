package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

/**
 * Internal request DTO for syncing BMW Series data from Vehicle Service
 * Used in event-driven cache updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwSeriesSyncRequestDto {
    
    @NotBlank(message = "Series code is required")
    @Size(max = 10, message = "Series code must not exceed 10 characters")
    private String code; // '3', 'X5', etc.
    
    @NotBlank(message = "Series name is required")
    @Size(max = 50, message = "Series name must not exceed 50 characters")
    private String name; // 'Seria 3', 'X5', etc.
    
    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder;
    
    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
