package live.alinmiron.beamerparts.product.controller.admin;

import live.alinmiron.beamerparts.product.dto.external.request.CreateProductRequestDto;
import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static live.alinmiron.beamerparts.product.controller.admin.AdminProductControllerOpenApiSpec.*;

/**
 * Admin REST controller for product management
 * These endpoints are exposed through the API Gateway as /api/admin/products/*
 * Used by admin users for product CRUD operations
 * 
 * Follows DDD architecture: Controller → Domain Services directly
 * - ProductCatalogDomainService: Core product business logic
 * TODO: Add security annotations when Spring Security is configured
 */
@RestController
@RequestMapping("/admin/products")
@Tag(name = "Admin Products", description = "Administrative product management API")
public class AdminProductController {
    
    private final ProductCatalogDomainService productCatalogDomainService;
    private final ProductMapper productMapper;
    
    public AdminProductController(ProductCatalogDomainService productCatalogDomainService,
                                ProductMapper productMapper) {
        this.productCatalogDomainService = productCatalogDomainService;
        this.productMapper = productMapper;
    }
    
    /**
     * Create a new product
     * Client: POST /api/admin/products
     */
    @PostMapping
    @Operation(
        summary = CREATE_PRODUCT_SUMMARY,
        description = CREATE_PRODUCT_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @RequestBody(
                description = REQUEST_BODY_CREATE_PRODUCT_DESCRIPTION,
                content = @Content(
                    examples = @ExampleObject(
                        name = "BMW F30 Brake Pad",
                        description = "Example of creating a BMW F30 brake pad product",
                        value = REQUEST_BODY_CREATE_PRODUCT_EXAMPLE
                    )
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateProductRequestDto request) {
        
        // Delegate to domain service for business logic
        Product product = productCatalogDomainService.createProduct(
                request.getName(),
                request.getSlug(),
                request.getSku(),
                request.getDescription(),
                request.getShortDescription(),
                request.getBasePrice(),
                request.getCategoryId(),
                request.getBrand(),
                request.getWeightGrams(),
                request.getDimensionsJson(),
                request.getIsFeatured(),
                request.getStatus()
        );
        
        // Map to admin DTO (TODO: Move to ProductMapper)
        ProductResponseDto productDto = productMapper.mapToAdminDto(product);
        
        return ResponseEntity.ok(ApiResponse.success(productDto));
    }
    
    /**
     * Update existing product
     * Client: PUT /api/admin/products/BMW-F30-AC-001
     */
    @PutMapping("/{sku}")
    @Operation(
        summary = UPDATE_PRODUCT_SUMMARY,
        description = UPDATE_PRODUCT_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @Parameter(
                description = PARAM_PRODUCT_SKU_DESCRIPTION,
                example = PARAM_PRODUCT_SKU_EXAMPLE,
                required = true
            )
            @PathVariable("sku") @NotBlank String sku,
            @RequestBody(
                description = REQUEST_BODY_UPDATE_PRODUCT_DESCRIPTION,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Update BMW F30 Brake Pad",
                        description = "Example of updating a BMW F30 brake pad product",
                        value = REQUEST_BODY_UPDATE_PRODUCT_EXAMPLE
                    )
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateProductRequestDto request) {
        
        // Delegate to domain service for business logic
        try {
            // First get the product to find its ID
            Product existingProduct = productCatalogDomainService.findProductBySku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
            
            // Update the product
            Product product = productCatalogDomainService.updateProduct(
                    existingProduct.getId(),
                    request.getName(),
                    request.getSlug(),
                    request.getSku(),
                    request.getDescription(),
                    request.getShortDescription(),
                    request.getBasePrice(),
                    request.getCategoryId(),
                    request.getBrand(),
                    request.getWeightGrams(),
                    request.getDimensionsJson(),
                    request.getIsFeatured(),
                    request.getStatus()
            );
            
            // Map to admin DTO (TODO: Move to ProductMapper)
            ProductResponseDto productDto = productMapper.mapToAdminDto(product);
            
            return ResponseEntity.ok(ApiResponse.success(productDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete product (soft delete - sets status to INACTIVE)
     * Client: DELETE /api/admin/products/BMW-F30-AC-001
     */
    @DeleteMapping("/{sku}")
    @Operation(
        summary = DELETE_PRODUCT_SUMMARY,
        description = DELETE_PRODUCT_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(
                description = PARAM_PRODUCT_SKU_DESCRIPTION,
                example = PARAM_PRODUCT_SKU_EXAMPLE,
                required = true
            )
            @PathVariable("sku") @NotBlank String sku) {
        
        // Delegate to domain service for business logic
        boolean deleted = productCatalogDomainService.deleteProductBySku(sku);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }


}
