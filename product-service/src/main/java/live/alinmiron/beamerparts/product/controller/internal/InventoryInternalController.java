package live.alinmiron.beamerparts.product.controller.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkStockCheckRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReservationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReleaseRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.StockReservationDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Internal REST controller for inventory-related service-to-service communication
 * These endpoints are NOT exposed through the API Gateway
 * Used primarily by User Service for cart operations and order processing
 */
@RestController
@RequestMapping("/internal/inventory")
@Tag(name = "Inventory Internal API", description = "Internal inventory operations for service-to-service communication")
public class InventoryInternalController {
    
    private final InventoryInternalService inventoryInternalService;
    
    public InventoryInternalController(InventoryInternalService inventoryInternalService) {
        this.inventoryInternalService = inventoryInternalService;
    }
    
    /**
     * Reserve stock for orders/cart operations
     * Used by: User Service (cart operations), Order Service
     */
    @Operation(summary = "Reserve stock", description = "Reserve inventory for orders and cart operations")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<StockReservationDto>> reserveStock(
            @Valid @RequestBody StockReservationRequestDto request) {
        
        StockReservationDto reservation = inventoryInternalService.reserveStock(request);
        
        return ResponseEntity.ok(ApiResponse.success(reservation));
    }
    
    /**
     * Release previously reserved stock
     * Used by: User Service (cart cleanup), Order Service (order cancellation)
     */
    @Operation(summary = "Release reserved stock", description = "Release previously reserved inventory")
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Valid @RequestBody StockReleaseRequestDto request) {
        
        inventoryInternalService.releaseStock(request);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Stock released successfully"));
    }
    
    /**
     * Bulk stock check for multiple items
     * Used by: User Service (cart validation), Order Service (order validation)
     */
    @Operation(summary = "Bulk stock check", description = "Check inventory levels for multiple items at once")
    @PostMapping("/bulk-check")
    public ResponseEntity<ApiResponse<List<InventoryInternalDto>>> bulkStockCheck(
            @Valid @RequestBody BulkStockCheckRequestDto request) {
        
        List<InventoryInternalDto> stockLevels = inventoryInternalService.bulkStockCheck(request);
        
        return ResponseEntity.ok(ApiResponse.success(stockLevels));
    }
    
    /**
     * Get inventory for specific product/variant
     * Used by: All services for stock level checks
     */
    @Operation(summary = "Get inventory by SKU", description = "Retrieve current inventory levels for product or variant")
    @GetMapping("/{sku}")
    public ResponseEntity<ApiResponse<InventoryInternalDto>> getInventory(
            @Parameter(description = "Product SKU identifier") @PathVariable String sku,
            @Parameter(description = "Optional variant SKU") @RequestParam(value = "variantSku", required = false) String variantSku) {
        
        InventoryInternalDto inventory = inventoryInternalService.getInventory(sku, variantSku);
        
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }
    
    /**
     * Update stock levels (admin operations)
     * Used by: Admin Service, external inventory systems
     */
    @Operation(summary = "Update stock levels", description = "Update inventory quantities with audit trail")
    @PutMapping("/{sku}/stock")
    public ResponseEntity<ApiResponse<InventoryInternalDto>> updateStock(
            @Parameter(description = "Product SKU identifier") @PathVariable String sku,
            @Parameter(description = "Optional variant SKU") @RequestParam(value = "variantSku", required = false) String variantSku,
            @Parameter(description = "New quantity amount") @RequestParam("quantity") Integer quantity,
            @Parameter(description = "Reason for stock change") @RequestParam("reason") String reason,
            @Parameter(description = "User performing the operation") @RequestParam("userCode") String userCode) {
        
        InventoryInternalDto updatedInventory = inventoryInternalService.updateStock(
                sku, variantSku, quantity, reason, userCode);
        
        return ResponseEntity.ok(ApiResponse.success(updatedInventory));
    }
    
    /**
     * Check if sufficient stock is available for quantity
     * Used by: User Service (add to cart validation)
     */
    @Operation(summary = "Check stock availability", description = "Verify if requested quantity is available in inventory")
    @GetMapping("/{sku}/available")
    public ResponseEntity<ApiResponse<Boolean>> isStockAvailable(
            @Parameter(description = "Product SKU identifier") @PathVariable String sku,
            @Parameter(description = "Optional variant SKU") @RequestParam(value = "variantSku", required = false) String variantSku,
            @Parameter(description = "Requested quantity") @RequestParam("quantity") Integer quantity) {
        
        boolean isAvailable = inventoryInternalService.isStockAvailable(sku, variantSku, quantity);
        
        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }
}
