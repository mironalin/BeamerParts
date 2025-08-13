package live.alinmiron.beamerparts.product.mapper;

import live.alinmiron.beamerparts.product.dto.external.response.InventoryResponseDto;
import live.alinmiron.beamerparts.product.entity.Inventory;
import org.springframework.stereotype.Component;

/**
 * Dedicated mapper component for Inventory entity to DTO conversions
 * Separates mapping logic from controllers for better maintainability
 */
@Component
public class InventoryMapper {

    /**
     * Maps Inventory entity to admin API response DTO
     * Uses rich domain model business methods for accurate calculations
     */
    public InventoryResponseDto mapToAdminDto(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponseDto.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .variantId(inventory.getVariant() != null ? inventory.getVariant().getId() : null)
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityReserved(inventory.getQuantityReserved())
                .minimumStockLevel(inventory.getMinimumStockLevel())
                .reorderPoint(inventory.getReorderPoint())
                .lastUpdated(inventory.getLastUpdated())
                // Use rich domain model business methods for accurate calculations
                .totalQuantity(inventory.getTotalQuantity())
                .isLowStock(inventory.isLowStock())
                .isOutOfStock(inventory.isOutOfStock())
                .isBelowMinimum(inventory.isBelowMinimum())
                .displayName(inventory.getDisplayName())
                .build();
    }
}
