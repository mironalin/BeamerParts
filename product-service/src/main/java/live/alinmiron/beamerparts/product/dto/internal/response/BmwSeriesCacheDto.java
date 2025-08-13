package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Internal DTO for BMW Series cache data
 * Used for fast compatibility lookups and service-to-service communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwSeriesCacheDto {
    
    private String code; // Primary key: '3', 'X5', etc.
    private String name; // 'Seria 3', 'X5', etc.
    private Integer displayOrder;
    private boolean isActive;
    private LocalDateTime lastUpdated;
}
