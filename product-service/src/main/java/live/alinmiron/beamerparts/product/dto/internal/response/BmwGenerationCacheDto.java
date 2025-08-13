package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal DTO for BMW Generation cache data
 * Used for fast compatibility lookups and service-to-service communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwGenerationCacheDto {
    
    private String code; // Primary key: 'F30', 'E90', etc.
    private String seriesCode; // Foreign key to bmw_series_cache
    private String name; // 'F30/F31/F34/F35', 'E90/E91/E92/E93', etc.
    private Integer yearStart;
    private Integer yearEnd; // null for current generation
    private List<String> bodyCodes; // ['F30', 'F31', 'F34', 'F35']
    private boolean isActive;
    private LocalDateTime lastUpdated;
    
    /**
     * Check if this generation is current (no end year)
     */
    public boolean isCurrent() {
        return yearEnd == null;
    }
    
    /**
     * Check if a specific year falls within this generation's range
     */
    public boolean includesYear(int year) {
        if (year < yearStart) return false;
        return yearEnd == null || year <= yearEnd;
    }
}
