package live.alinmiron.beamerparts.product.controller.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkProductRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.ProductValidationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductValidationDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.service.internal.ProductInternalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Internal REST controller for product-related service-to-service communication
 * These endpoints are NOT exposed through the API Gateway
 * Used by User Service, Vehicle Service, and other internal services
 */
@RestController
@RequestMapping("/internal/products")
@Tag(name = "Product Internal API", description = "Internal product operations for service-to-service communication")
public class ProductInternalController {
    
    private final ProductInternalService productInternalService;
    
    public ProductInternalController(ProductInternalService productInternalService) {
        this.productInternalService = productInternalService;
    }
    
    /**
     * Get single product by SKU for internal service communication
     * Used by: User Service (cart operations), Order Service
     */
    @Operation(summary = "Get product by SKU", description = "Retrieve product details for internal service communication")
    @GetMapping("/{sku}")
    public ResponseEntity<ApiResponse<ProductInternalDto>> getProductBySku(
            @Parameter(description = "Product SKU identifier") @PathVariable String sku,
            @Parameter(description = "Include inventory data") @RequestParam(value = "includeInventory", defaultValue = "false") boolean includeInventory,
            @Parameter(description = "Include product variants") @RequestParam(value = "includeVariants", defaultValue = "false") boolean includeVariants,
            @Parameter(description = "Include compatibility data") @RequestParam(value = "includeCompatibility", defaultValue = "false") boolean includeCompatibility) {
        
        ProductInternalDto product = productInternalService.getProductBySku(
                sku, includeInventory, includeVariants, includeCompatibility);
        
        return ResponseEntity.ok(ApiResponse.success(product));
    }
    
    /**
     * Get multiple products by SKUs (bulk operation)
     * Used by: User Service (cart validation), Order Service (order processing)
     */
    @Operation(summary = "Get products in bulk", description = "Retrieve multiple products by SKU list for batch operations")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ProductInternalDto>>> getProductsBulk(
            @Valid @RequestBody BulkProductRequestDto request) {
        
        List<ProductInternalDto> products = productInternalService.getProductsBulk(request);
        
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    /**
     * Validate product data for other services
     * Used by: User Service (cart operations), Admin operations
     */
    @Operation(summary = "Validate products", description = "Validate product existence and constraints for other services")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<List<ProductValidationDto>>> validateProducts(
            @Valid @RequestBody List<ProductValidationRequestDto> request) {
        
        List<ProductValidationDto> validationResults = productInternalService.validateProducts(request);
        
        return ResponseEntity.ok(ApiResponse.success(validationResults));
    }
    
    /**
     * Get products by BMW generation code
     * Used by: Vehicle Service (compatibility queries)
     */
    @Operation(summary = "Get products by generation", description = "Find products compatible with specific BMW generation")
    @GetMapping("/by-generation/{generationCode}")
    public ResponseEntity<ApiResponse<List<ProductInternalDto>>> getProductsByGeneration(
            @Parameter(description = "BMW generation code") @PathVariable String generationCode,
            @Parameter(description = "Include inventory data") @RequestParam(value = "includeInventory", defaultValue = "false") boolean includeInventory) {
        
        List<ProductInternalDto> products = productInternalService.getProductsByGeneration(
                generationCode, includeInventory);
        
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    /**
     * Check if product exists and is active
     * Used by: All services for quick existence checks
     */
    @Operation(summary = "Check product exists", description = "Verify if product exists and is active")
    @GetMapping("/{sku}/exists")
    public ResponseEntity<ApiResponse<Boolean>> productExists(@Parameter(description = "Product SKU identifier") @PathVariable String sku) {
        
        boolean exists = productInternalService.productExistsAndActive(sku);
        
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
