package live.alinmiron.beamerparts.product.controller.external;

import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static live.alinmiron.beamerparts.product.controller.external.ProductControllerOpenApiSpec.*;

/**
 * External REST controller for product catalog browsing
 * These endpoints are exposed through the API Gateway as /api/products/*
 * Used by client applications for product discovery and browsing
 * 
 * Follows DDD architecture: Controller → Domain Services directly
 * - ProductCatalogDomainService: Core product business logic
 * - InventoryInternalService: Stock information when needed
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Product catalog API for external clients")
public class ProductController {
    
    private final ProductCatalogDomainService productCatalogDomainService;
    private final ProductMapper productMapper;
    private final InventoryInternalService inventoryInternalService;
    
    public ProductController(ProductCatalogDomainService productCatalogDomainService,
                           ProductMapper productMapper,
                           InventoryInternalService inventoryInternalService) {
        this.productCatalogDomainService = productCatalogDomainService;
        this.productMapper = productMapper;
        this.inventoryInternalService = inventoryInternalService;
    }
    
    /**
     * Get all products with pagination and filtering
     * Client: GET /api/products?page=0&size=20&category=brake-system&status=ACTIVE
     */
        @GetMapping
    @Operation(summary = "Get all products",
            description = "Retrieve a paginated list of products with optional filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
            @Parameter(description = "Filter by category code") 
            @RequestParam(value = "categoryCode", required = false) String categoryCode,
            @Parameter(description = "Filter by product status") 
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by featured products only") 
            @RequestParam(value = "featured", required = false) Boolean featured,
            @ParameterObject Pageable pageable) {
        
        // Delegate to domain service for business logic
        Page<Product> products = productCatalogDomainService.findAllProducts(pageable);
        
        // Map to external DTOs  
        Page<ProductResponseDto> productDtos = products.map(productMapper::mapToExternalDto);
        
        return ResponseEntity.ok(ApiResponse.success(productDtos));
    }
    
    /**
     * Get single product by SKU with full details
     * Client: GET /api/products/BMW-F30-AC-001
     */
    @GetMapping("/{sku}")
    @Operation(
        summary = GET_PRODUCT_BY_SKU_SUMMARY, 
        description = GET_PRODUCT_BY_SKU_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySku(
            @Parameter(
                description = PARAM_SKU_DESCRIPTION, 
                example = PARAM_SKU_EXAMPLE, 
                required = true
            )
            @PathVariable("sku") @NotBlank String sku) {
        
        // Delegate to domain service for business logic
        Optional<Product> productOpt = productCatalogDomainService.findProductBySku(sku);
        
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Map to external DTO
        ProductResponseDto productDto = productMapper.mapToExternalDto(productOpt.get());
        
        return ResponseEntity.ok(ApiResponse.success(productDto));
    }
    
    /**
     * Search products by query string
     * Client: GET /api/products/search?q=brake+pad&page=0&size=10
     */
    @GetMapping("/search")
    @Operation(
        summary = SEARCH_PRODUCTS_SUMMARY,
        description = SEARCH_PRODUCTS_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @Parameter(
                description = PARAM_SEARCH_QUERY_DESCRIPTION,
                example = PARAM_SEARCH_QUERY_EXAMPLE
            )
            @RequestParam("q") @NotBlank String query,
            @Parameter(
                description = PARAM_CATEGORY_CODE_DESCRIPTION,
                example = PARAM_CATEGORY_CODE_EXAMPLE
            )
            @RequestParam(value = "category", required = false) String categoryCode,
            @Parameter(
                description = PARAM_MIN_PRICE_DESCRIPTION,
                example = PARAM_MIN_PRICE_EXAMPLE
            )
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @Parameter(
                description = PARAM_MAX_PRICE_DESCRIPTION,
                example = PARAM_MAX_PRICE_EXAMPLE
            )
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @ParameterObject Pageable pageable) {
        
        // Delegate to domain service for business logic
        Page<Product> products = productCatalogDomainService.searchProducts(query, pageable);
        
        // Map to external DTOs
        Page<ProductResponseDto> productDtos = products.map(productMapper::mapToExternalDto);
        
        return ResponseEntity.ok(ApiResponse.success(productDtos));
    }
    
    /**
     * Get featured products
     * Client: GET /api/products/featured?limit=10
     */
    @GetMapping("/featured")
    @Operation(
        summary = GET_FEATURED_PRODUCTS_SUMMARY,
        description = GET_FEATURED_PRODUCTS_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getFeaturedProducts(
            @Parameter(
                description = PARAM_LIMIT_DESCRIPTION,
                example = PARAM_LIMIT_EXAMPLE
            )
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        // Get all products and filter featured ones
        // TODO: Add findFeaturedProducts method to domain service
        List<Product> allProducts = productCatalogDomainService.findProductsBulk(List.of());
        List<Product> products = allProducts.stream()
                .filter(Product::getIsFeatured)
                .collect(Collectors.toList());
        
        // Apply limit if needed
        if (limit > 0 && products.size() > limit) {
            products = products.subList(0, limit);
        }
        
        // Map to external DTOs
        List<ProductResponseDto> productDtos = products.stream()
                .map(productMapper::mapToExternalDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(productDtos));
    }
    
    /**
     * Get products compatible with specific BMW generation
     * Client: GET /api/products/by-generation/F30?page=0&size=20
     */
    @GetMapping("/by-generation/{generationCode}")
    @Operation(
        summary = GET_PRODUCTS_BY_GENERATION_SUMMARY,
        description = GET_PRODUCTS_BY_GENERATION_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByGeneration(
            @Parameter(
                description = PARAM_GENERATION_CODE_DESCRIPTION,
                example = PARAM_GENERATION_CODE_EXAMPLE,
                required = true
            )
            @PathVariable("generationCode") @NotBlank String generationCode,
            @Parameter(
                description = PARAM_CATEGORY_BY_GENERATION_DESCRIPTION,
                example = PARAM_CATEGORY_BY_GENERATION_EXAMPLE
            )
            @RequestParam(value = "category", required = false) String categoryCode) {
        
        // Delegate to domain service for business logic  
        List<Product> products = productCatalogDomainService.findProductsByBmwGeneration(generationCode);
        
        // Map to external DTOs
        List<ProductResponseDto> productDtos = products.stream()
                .map(productMapper::mapToExternalDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(productDtos));
    }


}
