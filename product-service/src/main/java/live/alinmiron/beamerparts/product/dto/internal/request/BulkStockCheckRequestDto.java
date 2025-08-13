package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Bulk inventory check request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStockCheckRequestDto {
    
    @NotEmpty(message = "Items list cannot be empty")
    @Size(max = 100, message = "Maximum 100 items can be checked at once")
    @Valid
    private List<StockCheckItemDto> items;
    
    private String source; // "cart", "checkout", "order"
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCheckItemDto {
        private String sku;
        private String variantSku;
        private Integer requestedQuantity;
    }
}
