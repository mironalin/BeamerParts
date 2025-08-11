package live.alinmiron.beamerparts.vehicle.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Create BMW Series request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBmwSeriesRequestDto {
    
    @NotBlank(message = "Series name is required")
    @Size(max = 50, message = "Series name must be less than 50 characters")
    private String name;
    
    @NotBlank(message = "Series code is required")
    @Size(max = 10, message = "Series code must be less than 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Series code must contain only uppercase letters and numbers")
    private String code;
    
    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be positive")
    private Integer displayOrder;
    
    @Builder.Default
    private Boolean isActive = true;
}
