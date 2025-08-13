package live.alinmiron.beamerparts.product.controller.external;

import live.alinmiron.beamerparts.product.dto.external.response.CategoryResponseDto;
import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.mapper.CategoryMapper;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
import live.alinmiron.beamerparts.product.service.domain.CategoryDomainService;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * External REST controller for category browsing
 * These endpoints are exposed through the API Gateway as /api/categories/*
 * Used by client applications for category navigation and product discovery
 * 
 * Follows DDD architecture: Controller → Domain Services directly
 * - CategoryDomainService: Core category business logic
 * - ProductCatalogDomainService: Product retrieval by category
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {
    
    private final CategoryDomainService categoryDomainService;
    private final ProductCatalogDomainService productCatalogDomainService;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    
    public CategoryController(CategoryDomainService categoryDomainService, 
                            ProductCatalogDomainService productCatalogDomainService,
                            CategoryMapper categoryMapper,
                            ProductMapper productMapper) {
        this.categoryDomainService = categoryDomainService;
        this.productCatalogDomainService = productCatalogDomainService;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }
    
    /**
     * Get all categories (returns root categories and their tree structure)
     * Client: GET /api/categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategories() {
        // Delegate to domain service for business logic
        List<Category> categories = categoryDomainService.findRootCategories();
        
        // Map to external DTOs (TODO: Move to CategoryMapper)
        List<CategoryResponseDto> categoryDtos = categories.stream()
                .map(categoryMapper::mapToExternalDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(categoryDtos));
    }
    
    /**
     * Get category by ID
     * Client: GET /api/categories/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable @NotNull Long id) {
        // Delegate to domain service for business logic
        Optional<Category> categoryOpt = categoryDomainService.findCategoryById(id);
        
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Map to external DTO (TODO: Move to CategoryMapper)
        CategoryResponseDto categoryDto = categoryMapper.mapToExternalDto(categoryOpt.get());
        
        return ResponseEntity.ok(ApiResponse.success(categoryDto));
    }
    
    /**
     * Get products in a specific category
     * Client: GET /api/categories/1/products?page=0&size=20
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByCategory(
            @PathVariable @NotNull Long id,
            Pageable pageable) {
        
        // Delegate to domain service for business logic
        Page<Product> products = productCatalogDomainService.findProductsByCategory(id, pageable);
        
        // Map to external DTOs (TODO: Move to ProductMapper)
        Page<ProductResponseDto> productDtos = products.map(productMapper::mapToExternalDto);
        
        return ResponseEntity.ok(ApiResponse.success(productDtos));
    }


}
