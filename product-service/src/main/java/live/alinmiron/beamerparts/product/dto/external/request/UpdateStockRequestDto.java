package live.alinmiron.beamerparts.product.dto.external.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating stock levels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating product inventory stock levels")
public class UpdateStockRequestDto {
    
    /**
     * The action to perform: ADD, REMOVE, SET
     */
    @Schema(
        description = "Type of stock operation to perform",
        example = "ADD",
        allowableValues = {"ADD", "REMOVE", "SET"}
    )
    @NotNull(message = "Action is required")
    private StockAction action;
    
    /**
     * Quantity to add, remove, or set to
     */
    @Schema(
        description = "Number of units to add, remove, or set. Must be non-negative.",
        example = "25"
    )
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;
    
    /**
     * Reason for the stock change
     */
    @Schema(
        description = "Reason for the inventory adjustment (optional)",
        example = "Initial stock replenishment from supplier"
    )
    private String reason;
    
    /**
     * Reference number (e.g., purchase order, adjustment ID)
     */
    @Schema(
        description = "Reference number for tracking (purchase order, adjustment ID, etc.)",
        example = "PO-2024-001"
    )
    private String reference;
    
    /**
     * Variant SKU if updating variant-specific inventory
     */
    @Schema(
        description = "Specific product variant SKU (optional, for products with variants)",
        example = "CHROME"
    )
    private String variantSku;
    
    @Schema(description = "Available stock update actions")
    public enum StockAction {
        @Schema(description = "Add quantity to current stock")
        ADD,    // Add to current stock
        
        @Schema(description = "Remove quantity from current stock")
        REMOVE, // Remove from current stock
        
        @Schema(description = "Set stock to exact quantity (overwrite current)")
        SET     // Set stock to exact amount
    }
}
