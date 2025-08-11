package live.alinmiron.beamerparts.vehicle.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Create BMW Generation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBmwGenerationRequestDto {
    
    @NotBlank(message = "Series code is required")
    @Size(max = 10, message = "Series code must be less than 10 characters")
    private String seriesCode;
    
    @NotBlank(message = "Generation name is required")
    @Size(max = 100, message = "Generation name must be less than 100 characters")
    private String name;
    
    @NotBlank(message = "Generation code is required")
    @Size(max = 20, message = "Generation code must be less than 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Generation code must contain only uppercase letters and numbers")
    private String code;
    
    @NotNull(message = "Start year is required")
    @Min(value = 1950, message = "Start year must be 1950 or later")
    private Integer yearStart;
    
    @Min(value = 1950, message = "End year must be 1950 or later")
    private Integer yearEnd;
    
    private String[] bodyCodes;
    
    @Builder.Default
    private Boolean isActive = true;
}
