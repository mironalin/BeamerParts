package live.alinmiron.beamerparts.product.mapper;

import live.alinmiron.beamerparts.product.dto.external.response.CategoryResponseDto;
import live.alinmiron.beamerparts.product.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Dedicated mapper component for Category entity to DTO conversions
 * Separates mapping logic from controllers for better maintainability
 */
@Component
public class CategoryMapper {

    /**
     * Maps Category entity to external API response DTO
     * Includes basic category information for client consumption
     */
    public CategoryResponseDto mapToExternalDto(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                // Real-time computed fields using entity business methods
                .hasSubcategories(category.hasSubcategories())
                .hasProducts(category.hasProducts())
                .build();
    }

    /**
     * Maps Category entity to admin API response DTO
     * Includes additional administrative information
     */
    public CategoryResponseDto mapToAdminDto(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                // Real-time computed fields using entity business methods
                .hasSubcategories(category.hasSubcategories())
                .hasProducts(category.hasProducts())
                .build();
    }
}
