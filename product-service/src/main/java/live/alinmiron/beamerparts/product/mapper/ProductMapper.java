package live.alinmiron.beamerparts.product.mapper;

import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dedicated mapper component for Product entity to DTO conversions
 * Separates mapping logic from controllers for better maintainability
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMapper {

    private final InventoryInternalService inventoryInternalService;

    /**
     * Maps Product entity to external API response DTO
     * Includes inventory information for client consumption
     */
    public ProductResponseDto mapToExternalDto(Product product) {
        if (product == null) {
            return null;
        }

        // Get comprehensive real-time inventory information
        Integer totalStock = 0;
        Integer reservedStock = 0;
        boolean isOutOfStock = true;
        boolean isLowStock = false;

        try {
            // Use comprehensive inventory method instead of just available quantity
            var inventoryDto = inventoryInternalService.getInventory(product.getSku(), null);
            
            totalStock = inventoryDto.getQuantityAvailable();
            reservedStock = inventoryDto.getQuantityReserved();
            isOutOfStock = !inventoryDto.isInStock();
            isLowStock = inventoryDto.isLowStock();
            
        } catch (Exception e) {
            log.warn("Failed to get inventory for product {}: {}", product.getSku(), e.getMessage());
            // Use defaults when inventory service fails (resilient behavior)
            totalStock = 0;
            reservedStock = 0;
            isOutOfStock = true;
            isLowStock = false;
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .basePrice(product.getBasePrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .brand(product.getBrand())
                .weightGrams(product.getWeightGrams())
                .dimensionsJson(product.getDimensionsJson())
                .isFeatured(product.getIsFeatured())
                .status(product.getStatus())
                .totalStock(totalStock)
                .reservedStock(reservedStock)
                .isOutOfStock(isOutOfStock)
                .isLowStock(isLowStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Maps Product entity to admin API response DTO
     * Includes comprehensive administrative information with real-time inventory
     */
    public ProductResponseDto mapToAdminDto(Product product) {
        if (product == null) {
            return null;
        }

        // Get comprehensive real-time inventory information for admin users
        Integer totalStock = 0;
        Integer reservedStock = 0;
        boolean isOutOfStock = true;
        boolean isLowStock = false;

        try {
            // Admin users need accurate inventory data for management decisions
            var inventoryDto = inventoryInternalService.getInventory(product.getSku(), null);
            
            totalStock = inventoryDto.getQuantityAvailable();
            reservedStock = inventoryDto.getQuantityReserved();
            isOutOfStock = !inventoryDto.isInStock();
            isLowStock = inventoryDto.isLowStock();
            
        } catch (Exception e) {
            log.warn("Failed to get inventory for product {} in admin view: {}", product.getSku(), e.getMessage());
            // Use defaults when inventory service fails (resilient behavior)
            totalStock = 0;
            reservedStock = 0;
            isOutOfStock = true;
            isLowStock = false;
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .basePrice(product.getBasePrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .brand(product.getBrand())
                .weightGrams(product.getWeightGrams())
                .dimensionsJson(product.getDimensionsJson())
                .isFeatured(product.getIsFeatured())
                .status(product.getStatus())
                // Real-time inventory data for comprehensive admin management
                .totalStock(totalStock)
                .reservedStock(reservedStock)
                .isOutOfStock(isOutOfStock)
                .isLowStock(isLowStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
