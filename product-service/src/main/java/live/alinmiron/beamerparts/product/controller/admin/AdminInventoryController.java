package live.alinmiron.beamerparts.product.controller.admin;

import live.alinmiron.beamerparts.product.dto.external.request.UpdateStockRequestDto;
import live.alinmiron.beamerparts.product.dto.external.response.InventoryResponseDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.mapper.InventoryMapper;
import live.alinmiron.beamerparts.product.service.domain.InventoryDomainService;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

import static live.alinmiron.beamerparts.product.controller.admin.AdminInventoryControllerOpenApiSpec.*;

/**
 * Admin REST controller for inventory management
 * These endpoints are exposed through the API Gateway as /api/admin/inventory/*
 * Used by admin users for stock management operations
 * 
 * Follows DDD architecture: Controller → Domain Services directly
 * - InventoryDomainService: Core inventory business logic
 * TODO: Add security annotations when Spring Security is configured
 */
@RestController
@RequestMapping("/admin/inventory")
@Tag(name = "Admin Inventory", description = "Administrative inventory management API")
public class AdminInventoryController {
    
    private final InventoryDomainService inventoryDomainService;
    private final ProductCatalogDomainService productCatalogDomainService;
    private final InventoryMapper inventoryMapper;
    
    public AdminInventoryController(InventoryDomainService inventoryDomainService,
                                  ProductCatalogDomainService productCatalogDomainService,
                                  InventoryMapper inventoryMapper) {
        this.inventoryDomainService = inventoryDomainService;
        this.productCatalogDomainService = productCatalogDomainService;
        this.inventoryMapper = inventoryMapper;
    }
    
    /**
     * Update stock levels for a product
     * Client: PUT /api/admin/inventory/BMW-F30-AC-001/stock
     */
    @PutMapping("/{sku}/stock")
    @Operation(
        summary = UPDATE_STOCK_SUMMARY,
        description = UPDATE_STOCK_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<InventoryResponseDto>> updateStock(
            @Parameter(
                description = PARAM_INVENTORY_SKU_DESCRIPTION,
                example = PARAM_INVENTORY_SKU_EXAMPLE,
                required = true
            )
            @PathVariable("sku") @NotBlank String sku,
            @Parameter(description = REQUEST_BODY_UPDATE_STOCK_DESCRIPTION)
            @Valid @RequestBody UpdateStockRequestDto request) {
        
        // Delegate to domain services for business logic
        try {
            // First, get the product
            Product product = productCatalogDomainService.findProductBySku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
            
            // Calculate new quantity based on action
            Integer currentQuantity = inventoryDomainService.getAvailableQuantity(product, request.getVariantSku());
            Integer newQuantity = calculateNewQuantity(currentQuantity, request.getAction(), request.getQuantity());
            
            // Adjust the stock
            inventoryDomainService.adjustStock(product, request.getVariantSku(), newQuantity, request.getReason());
            
            // Get updated inventory
            Inventory inventory = inventoryDomainService.getInventory(product, request.getVariantSku())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found after adjustment"));
            
            // Map to admin DTO (TODO: Move to InventoryMapper)
            InventoryResponseDto inventoryDto = inventoryMapper.mapToAdminDto(inventory);
            
            return ResponseEntity.ok(ApiResponse.success(inventoryDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get inventory details for a product
     * Client: GET /api/admin/inventory/BMW-F30-AC-001
     */
    @GetMapping("/{sku}")
    @Operation(
        summary = GET_INVENTORY_SUMMARY,
        description = GET_INVENTORY_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventory(
            @Parameter(
                description = PARAM_INVENTORY_SKU_DESCRIPTION,
                example = PARAM_INVENTORY_SKU_EXAMPLE,
                required = true
            )
            @PathVariable("sku") @NotBlank String sku) {
        
        // Delegate to domain services for business logic
        try {
            // First, get the product
            Product product = productCatalogDomainService.findProductBySku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
            
            // Get inventory
            Optional<Inventory> inventoryOpt = inventoryDomainService.getInventory(product, null);
            
            if (inventoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Map to admin DTO (TODO: Move to InventoryMapper)
            InventoryResponseDto inventoryDto = inventoryMapper.mapToAdminDto(inventoryOpt.get());
            
            return ResponseEntity.ok(ApiResponse.success(inventoryDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }



    /**
     * Calculate new quantity based on stock action
     */
    private Integer calculateNewQuantity(Integer currentQuantity, UpdateStockRequestDto.StockAction action, Integer quantity) {
        return switch (action) {
            case ADD -> currentQuantity + quantity;
            case REMOVE -> Math.max(0, currentQuantity - quantity); // Prevent negative stock
            case SET -> quantity;
        };
    }
}
