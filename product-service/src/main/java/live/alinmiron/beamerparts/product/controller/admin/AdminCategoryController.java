package live.alinmiron.beamerparts.product.controller.admin;

import live.alinmiron.beamerparts.product.dto.external.request.CreateCategoryRequestDto;
import live.alinmiron.beamerparts.product.dto.external.response.CategoryResponseDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.mapper.CategoryMapper;
import live.alinmiron.beamerparts.product.service.domain.CategoryDomainService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static live.alinmiron.beamerparts.product.controller.admin.AdminCategoryControllerOpenApiSpec.*;

/**
 * Admin REST controller for category management
 * These endpoints are exposed through the API Gateway as /api/admin/categories/*
 * Used by admin users for category CRUD operations
 * 
 * Follows DDD architecture: Controller → Domain Services directly
 * - CategoryDomainService: Core category business logic
 * TODO: Add security annotations when Spring Security is configured
 */
@RestController
@RequestMapping("/admin/categories")
@Tag(name = "Admin Categories", description = "Administrative category management API")
public class AdminCategoryController {
    
    private final CategoryDomainService categoryDomainService;
    private final CategoryMapper categoryMapper;
    
    public AdminCategoryController(CategoryDomainService categoryDomainService,
                                 CategoryMapper categoryMapper) {
        this.categoryDomainService = categoryDomainService;
        this.categoryMapper = categoryMapper;
    }
    
    /**
     * Create a new category
     * Client: POST /api/admin/categories
     */
    @PostMapping
    @Operation(
        summary = CREATE_CATEGORY_SUMMARY,
        description = CREATE_CATEGORY_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @RequestBody(
                description = REQUEST_BODY_CREATE_CATEGORY_DESCRIPTION,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Brake System Category",
                        description = "Example of creating a brake system category",
                        value = REQUEST_BODY_CREATE_CATEGORY_EXAMPLE
                    )
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateCategoryRequestDto request) {
        
        // Delegate to domain service for business logic
        Category category = categoryDomainService.createCategory(
                request.getName(),
                request.getSlug(),
                request.getDescription(),
                request.getParentId(),
                request.getDisplayOrder(),
                request.getIsActive()
        );
        
        // Map to admin DTO (TODO: Move to CategoryMapper)
        CategoryResponseDto categoryDto = categoryMapper.mapToAdminDto(category);
        
        return ResponseEntity.ok(ApiResponse.success(categoryDto));
    }
    
    /**
     * Update existing category
     * Client: PUT /api/admin/categories/{id}
     */
    @PutMapping("/{id}")
    @Operation(
        summary = UPDATE_CATEGORY_SUMMARY,
        description = UPDATE_CATEGORY_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @Parameter(
                description = PARAM_CATEGORY_ID_DESCRIPTION,
                example = PARAM_CATEGORY_ID_EXAMPLE,
                required = true
            )
            @PathVariable("id") @NotNull Long id,
            @RequestBody(
                description = REQUEST_BODY_UPDATE_CATEGORY_DESCRIPTION,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Update Brake System Category",
                        description = "Example of updating a brake system category",
                        value = REQUEST_BODY_UPDATE_CATEGORY_EXAMPLE
                    )
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateCategoryRequestDto request) {
        
        // Delegate to domain service for business logic
        try {
            Category category = categoryDomainService.updateCategory(
                    id,
                    request.getName(),
                    request.getSlug(),
                    request.getDescription(),
                    request.getParentId(),
                    request.getDisplayOrder(),
                    request.getIsActive()
            );
            
            // Map to admin DTO (TODO: Move to CategoryMapper)
            CategoryResponseDto categoryDto = categoryMapper.mapToAdminDto(category);
            
            return ResponseEntity.ok(ApiResponse.success(categoryDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete category (soft delete)
     * Client: DELETE /api/admin/categories/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = DELETE_CATEGORY_SUMMARY,
        description = DELETE_CATEGORY_DESCRIPTION
    )
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(
                description = PARAM_DELETE_CATEGORY_ID_DESCRIPTION,
                example = PARAM_DELETE_CATEGORY_ID_EXAMPLE,
                required = true
            )
            @PathVariable("id") @NotNull Long id) {
        
        // Delegate to domain service for business logic
        try {
            categoryDomainService.deleteCategory(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Cannot delete category: " + e.getMessage(), null));
        }
    }


}
