package live.alinmiron.beamerparts.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Add vehicle request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVehicleRequestDto {
    
    @NotBlank(message = "Series code is required")
    @Size(max = 10, message = "Series code must be less than 10 characters")
    private String seriesCode;
    
    @NotBlank(message = "Generation code is required")
    @Size(max = 20, message = "Generation code must be less than 20 characters")
    private String generationCode;
    
    @Min(value = 1950, message = "Year must be 1950 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer year;
    
    @Size(max = 100, message = "Model variant must be less than 100 characters")
    private String modelVariant;
    
    @Builder.Default
    private Boolean isPrimary = false;
}
