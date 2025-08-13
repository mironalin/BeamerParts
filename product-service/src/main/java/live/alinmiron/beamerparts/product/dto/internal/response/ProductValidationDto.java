package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Internal DTO for product validation results
 * Used by other services to validate product data in cart operations, orders, etc.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductValidationDto {
    
    private String sku;
    private String variantSku;
    private boolean exists;
    private boolean isActive;
    private boolean isAvailable;
    
    // Product details if valid
    private String name;
    private BigDecimal currentPrice;
    private String status;
    
    // Inventory info if requested
    private Integer availableQuantity;
    private boolean isInStock;
    
    // Error details if invalid
    private String errorCode;
    private String errorMessage;
    
    /**
     * Check if product is valid for operations (exists, active, available)
     */
    public boolean isValid() {
        return exists && isActive && isAvailable;
    }
    
    /**
     * Check if product can fulfill requested quantity
     */
    public boolean canFulfillQuantity(int requestedQuantity) {
        return isValid() && availableQuantity != null && availableQuantity >= requestedQuantity;
    }
    
    /**
     * Create error validation result
     */
    public static ProductValidationDto error(String sku, String errorCode, String errorMessage) {
        return ProductValidationDto.builder()
                .sku(sku)
                .exists(false)
                .isActive(false)
                .isAvailable(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Create success validation result
     */
    public static ProductValidationDto success(String sku, String name, BigDecimal price, 
                                               String status, Integer availableQuantity) {
        return ProductValidationDto.builder()
                .sku(sku)
                .exists(true)
                .isActive("ACTIVE".equals(status))
                .isAvailable(availableQuantity != null && availableQuantity > 0)
                .name(name)
                .currentPrice(price)
                .status(status)
                .availableQuantity(availableQuantity)
                .isInStock(availableQuantity != null && availableQuantity > 0)
                .build();
    }
}