package live.alinmiron.beamerparts.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User vehicle DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleResponseDto {
    
    private Long id;
    private Long userId;
    private String seriesCode;
    private String generationCode;
    private Integer year;
    private String modelVariant;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    
    // Computed fields
    private String displayName;
    private Boolean isCurrentGeneration;
    
    // Optional extended info (when fetched with vehicle service data)
    private String seriesName; // "3 Series", "X5", etc.
    private String generationName; // "E46", "F30/F31/F34/F35", etc.
}
