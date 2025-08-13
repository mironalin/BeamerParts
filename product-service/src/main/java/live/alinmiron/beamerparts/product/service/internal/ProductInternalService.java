package live.alinmiron.beamerparts.product.service.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkProductRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.ProductValidationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductValidationDto;
import live.alinmiron.beamerparts.product.entity.Product;


import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Internal Product Service for service-to-service communication
 * Thin orchestration layer that delegates business logic to ProductCatalogDomainService
 * 
 * This service handles:
 * - DTO mapping and transformation
 * - API contract fulfillment 
 * - Coordination between domain services
 * 
 * Business logic is handled by ProductCatalogDomainService following DDD principles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductInternalService {
    
    private final ProductCatalogDomainService productCatalogDomainService;
    private final InventoryInternalService inventoryInternalService;
    
    /**
     * Get product by SKU for internal API (delegates to domain service)
     */
    public ProductInternalDto getProductBySku(String sku, boolean includeInventory, 
                                              boolean includeVariants, boolean includeCompatibility) {
        log.debug("Getting product by SKU: {}", sku);
        
        // Delegate to domain service for business logic
        Optional<Product> productOpt = productCatalogDomainService.findProductBySku(sku);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found: " + sku);
        }
        
        return mapToInternalDto(productOpt.get(), includeInventory, includeVariants, includeCompatibility);
    }
    
    /**
     * Bulk get products by SKUs (delegates to domain service)
     */
    public List<ProductInternalDto> getProductsBulk(BulkProductRequestDto request) {
        log.debug("Getting products by SKUs: {}", request.getSkus());
        
        // Delegate to domain service for business logic and validation
        List<Product> products = productCatalogDomainService.findProductsBulk(request.getSkus());
        
        return products.stream()
                .map(product -> mapToInternalDto(product, 
                        request.getIncludeInventory(),
                        request.getIncludeVariants(),
                        request.getIncludeCompatibility()))
                .collect(Collectors.toList());
    }
    
    /**
     * Validate products for cart/order operations (delegates to domain service)
     */
    public List<ProductValidationDto> validateProducts(List<ProductValidationRequestDto> request) {
        log.debug("Validating {} products", request.size());
        
        return request.stream()
                .map(item -> {
                    try {
                        // Delegate to domain service for business validation
                        ProductCatalogDomainService.ProductValidationResult domainResult = 
                            productCatalogDomainService.validateProductForTransaction(item.getSku());
                        
                        ProductValidationDto.ProductValidationDtoBuilder validation = ProductValidationDto.builder()
                                .sku(item.getSku())
                                .variantSku(item.getVariantSku())
                                .exists(domainResult.exists())
                                .isActive(domainResult.isValid())
                                .isAvailable(domainResult.isValid());
                        
                        if (!domainResult.exists()) {
                            return validation
                                    .errorMessage("Product not found")
                                    .build();
                        }
                        
                        if (!domainResult.isValid()) {
                            Product product = domainResult.getProduct();
                            return validation
                                    .name(product != null ? product.getName() : null)
                                    .currentPrice(product != null ? product.getBasePrice() : null)
                                    .errorMessage(domainResult.getErrorMessage())
                                    .build();
                        }
                        
                        // Product is valid, populate full details
                        Product product = domainResult.getProduct();
                        
                        // Check inventory if quantity requested
                        if (item.getRequestedQuantity() != null && item.getRequestedQuantity() > 0) {
                            boolean hasStock = inventoryInternalService.checkStockAvailability(
                                    item.getSku(), item.getVariantSku(), item.getRequestedQuantity());
                            
                            Integer availableQuantity = inventoryInternalService.getAvailableQuantity(
                                    item.getSku(), item.getVariantSku());
                            
                            return validation
                                    .isAvailable(hasStock)
                                    .availableQuantity(availableQuantity)
                                    .currentPrice(product.getBasePrice())
                                    .name(product.getName())
                                    .build();
                        } else {
                            // Just validate product existence and status
                            return validation
                                    .currentPrice(product.getBasePrice())
                                    .name(product.getName())
                                    .build();
                        }
                        
                    } catch (Exception e) {
                        log.error("Error validating product {}: {}", item.getSku(), e.getMessage());
                        return ProductValidationDto.builder()
                                .sku(item.getSku())
                                .variantSku(item.getVariantSku())
                                .exists(false)
                                .isActive(false)
                                .isAvailable(false)
                                .errorMessage("Validation error: " + e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get products by BMW generation code (delegates to domain service)
     */
    public List<ProductInternalDto> getProductsByGeneration(String generationCode, boolean includeInventory) {
        log.debug("Getting products by generation: {}", generationCode);
        
        // Delegate to domain service for business logic and validation
        // Domain service handles empty/null generation codes correctly
        List<Product> products = productCatalogDomainService.findProductsByBmwGeneration(generationCode);
        
        return products.stream()
                .map(product -> mapToInternalDto(product, includeInventory, false, false))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if product exists and is active (delegates to domain service)
     */
    public boolean productExistsAndActive(String sku) {
        // Delegate to domain service for business logic
        return productCatalogDomainService.isProductAvailableForPurchase(sku);
    }
    
    /**
     * Check if product exists and is active (legacy method name - delegates to main method)
     */
    public boolean isProductValid(String sku) {
        return productExistsAndActive(sku);
    }
    
    /**
     * Map Product entity to Internal DTO with optional includes
     */
    private ProductInternalDto mapToInternalDto(Product product, boolean includeInventory, 
                                               boolean includeVariants, boolean includeCompatibility) {
        ProductInternalDto.ProductInternalDtoBuilder builder = ProductInternalDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .basePrice(product.getBasePrice())
                .status(product.getStatus().name())
                .brand(product.getBrand())
                .weightGrams(product.getWeightGrams())
                .shortDescription(product.getShortDescription())
                .categoryName(product.getCategory().getName())
                .categoryId(product.getCategory().getId())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .isFeatured(product.getIsFeatured());
        
        // Include inventory if requested
        if (includeInventory) {
            builder.inventory(inventoryInternalService.getInventory(product.getSku(), null));
        }
        
        // TODO: Add variants and compatibility if requested when those services are implemented
        
        return builder.build();
    }
    
    /**
     * Map Product entity to Internal DTO (legacy method without includes)
     */
}
