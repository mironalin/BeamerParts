package live.alinmiron.beamerparts.product.dto.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for BMW cache bulk synchronization.
 * Combines both series and generation payloads into a single request body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmwCacheBulkSyncRequestDto {

    /**
     * List of BMW series cache entries to sync.
     */
    @Valid
    @NotNull
    private List<BmwSeriesSyncRequestDto> seriesData;

    /**
     * List of BMW generation cache entries to sync.
     */
    @Valid
    @NotNull
    private List<BmwGenerationSyncRequestDto> generationData;
}


