package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * Internal request DTO for syncing BMW Generation data from Vehicle Service
 * Used in event-driven cache updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwGenerationSyncRequestDto {
    
    @NotBlank(message = "Generation code is required")
    @Size(max = 20, message = "Generation code must not exceed 20 characters")
    private String code; // 'F30', 'E90', etc.
    
    @NotBlank(message = "Series code is required")
    @Size(max = 10, message = "Series code must not exceed 10 characters")
    private String seriesCode; // '3', 'X5', etc.
    
    @NotBlank(message = "Generation name is required")
    @Size(max = 100, message = "Generation name must not exceed 100 characters")
    private String name; // 'F30/F31/F34/F35', etc.
    
    @NotNull(message = "Year start is required")
    @Min(value = 1950, message = "Year start must be 1950 or later")
    private Integer yearStart;
    
    @Min(value = 1950, message = "Year end must be 1950 or later")
    private Integer yearEnd; // null for current generation
    
    private List<String> bodyCodes; // ['F30', 'F31', 'F34', 'F35']
    
    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
