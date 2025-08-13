package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductVariant;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Domain service that encapsulates all product variant business logic.
 * 
 * Business Rules:
 * - Variant SKU suffixes must be unique within a product
 * - Variants can only be created for existing products
 * - Variant names should be descriptive and meaningful
 * - Price modifiers can be positive (surcharge) or negative (discount)
 * - Variants inherit base properties from their parent product
 * - Variant activation/deactivation affects inventory availability
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductVariantDomainService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new product variant with business validation
     * 
     * Business Rules:
     * - Product must exist
     * - SKU suffix must be unique within the product
     * - Variant inherits product's base properties
     */
    @Transactional
    public ProductVariant createProductVariant(Long productId, String name, String skuSuffix,
                                             BigDecimal priceModifier, Boolean isActive) {
        log.debug("Creating product variant for product ID: {}", productId);
        
        // Business Rule: Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        // Business Rule: Validate unique SKU suffix for this product
        if (productVariantRepository.existsByProductAndSkuSuffix(product, skuSuffix)) {
            throw new IllegalArgumentException("Variant with SKU suffix '" + skuSuffix + "' already exists for this product");
        }
        
        // Create variant
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(name)
                .skuSuffix(skuSuffix)
                .priceModifier(priceModifier)
                .isActive(isActive)
                .build();
        
        variant = productVariantRepository.save(variant);
        
        log.info("Created product variant: {} for product: {}", variant.getFullSku(), product.getName());
        
        return variant;
    }

    /**
     * Update product variant with business validation
     * 
     * Business Rules:
     * - SKU suffix must remain unique within the product if changed
     * - Cannot change the parent product of a variant
     * - All other properties can be updated
     */
    @Transactional
    public ProductVariant updateProductVariant(Long variantId, String name, String skuSuffix,
                                             BigDecimal priceModifier, Boolean isActive) {
        log.debug("Updating product variant ID: {}", variantId);
        
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found with ID: " + variantId));
        
        // Business Rule: Validate unique SKU suffix (if changing)
        if (!variant.getSkuSuffix().equals(skuSuffix) && 
            productVariantRepository.existsByProductAndSkuSuffix(variant.getProduct(), skuSuffix)) {
            throw new IllegalArgumentException("Variant with SKU suffix '" + skuSuffix + "' already exists for this product");
        }
        
        // Update fields
        variant.setName(name);
        variant.setSkuSuffix(skuSuffix);
        variant.setPriceModifier(priceModifier);
        variant.setIsActive(isActive);
        
        variant = productVariantRepository.save(variant);
        
        log.info("Updated product variant: {}", variant.getFullSku());
        
        return variant;
    }

    /**
     * Activate product variant
     * 
     * Business Rule: Activation makes variant available for sale
     */
    @Transactional
    public ProductVariant activateProductVariant(Long variantId) {
        log.debug("Activating product variant ID: {}", variantId);
        
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found with ID: " + variantId));
        
        variant.setIsActive(true);
        variant = productVariantRepository.save(variant);
        
        log.info("Activated product variant: {}", variant.getFullSku());
        
        return variant;
    }

    /**
     * Deactivate product variant
     * 
     * Business Rule: Deactivation removes variant from sale but preserves data
     */
    @Transactional
    public ProductVariant deactivateProductVariant(Long variantId) {
        log.debug("Deactivating product variant ID: {}", variantId);
        
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found with ID: " + variantId));
        
        variant.setIsActive(false);
        variant = productVariantRepository.save(variant);
        
        log.info("Deactivated product variant: {}", variant.getFullSku());
        
        return variant;
    }

    /**
     * Delete product variant
     * 
     * Business Rule: Hard delete - removes variant completely
     * Note: This should only be used when variant has no associated data
     */
    @Transactional
    public void deleteProductVariant(Long variantId) {
        log.debug("Deleting product variant ID: {}", variantId);
        
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found with ID: " + variantId));
        
        productVariantRepository.delete(variant);
        
        log.info("Deleted product variant: {}", variant.getFullSku());
    }

    /**
     * Find product variant by ID
     */
    public Optional<ProductVariant> findProductVariantById(Long variantId) {
        log.debug("Finding product variant by ID: {}", variantId);
        return productVariantRepository.findById(variantId);
    }

    /**
     * Find product variant by product and SKU suffix
     */
    public Optional<ProductVariant> findProductVariantBySkuSuffix(Long productId, String skuSuffix) {
        log.debug("Finding product variant by product ID: {} and SKU suffix: {}", productId, skuSuffix);
        return productVariantRepository.findByProductIdAndSkuSuffix(productId, skuSuffix);
    }

    /**
     * Find all variants for a specific product
     */
    public List<ProductVariant> findVariantsByProduct(Long productId) {
        log.debug("Finding variants for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        return productVariantRepository.findByProduct(product);
    }

    /**
     * Find active variants for a specific product
     */
    public List<ProductVariant> findActiveVariantsByProduct(Long productId) {
        log.debug("Finding active variants for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        return productVariantRepository.findByProductAndIsActive(product, true);
    }

    /**
     * Search product variants by name (case-insensitive)
     */
    public List<ProductVariant> searchVariantsByName(String name) {
        log.debug("Searching product variants by name: {}", name);
        return productVariantRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Find variants that have inventory/stock
     */
    public List<ProductVariant> findVariantsWithStock() {
        log.debug("Finding variants with stock");
        return productVariantRepository.findVariantsWithStock();
    }

    /**
     * Find variants with low stock levels
     */
    public List<ProductVariant> findVariantsWithLowStock() {
        log.debug("Finding variants with low stock");
        return productVariantRepository.findVariantsWithLowStock();
    }

    /**
     * Get all product variants with pagination
     */
    public Page<ProductVariant> findAllProductVariants(Pageable pageable) {
        log.debug("Finding all product variants with pagination");
        return productVariantRepository.findAll(pageable);
    }

    /**
     * Check if variant exists by ID
     */
    public boolean existsById(Long variantId) {
        return productVariantRepository.existsById(variantId);
    }

    /**
     * Check if SKU suffix exists for a specific product
     * 
     * Business Rule: SKU suffixes must be unique within a product
     */
    public boolean isSkuSuffixUniqueForProduct(Long productId, String skuSuffix) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        return !productVariantRepository.existsByProductAndSkuSuffix(product, skuSuffix);
    }

    /**
     * Validate variant for inventory operations
     * 
     * Business Rule: Only active variants can have inventory operations
     */
    public void validateVariantForInventoryOperations(Long variantId) {
        if (variantId == null) {
            return; // Base product inventory is allowed
        }
        
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found with ID: " + variantId));
        
        if (!variant.getIsActive()) {
            throw new IllegalArgumentException("Cannot perform inventory operations on inactive variant");
        }
    }

    /**
     * Get variant statistics for a product
     */
    public VariantStats getVariantStatsForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        List<ProductVariant> allVariants = productVariantRepository.findByProduct(product);
        long activeVariants = allVariants.stream().mapToLong(v -> v.getIsActive() ? 1 : 0).sum();
        
        return new VariantStats(allVariants.size(), activeVariants);
    }

    /**
     * Value object for variant statistics
     */
    public static class VariantStats {
        private final long totalVariants;
        private final long activeVariants;
        
        public VariantStats(long totalVariants, long activeVariants) {
            this.totalVariants = totalVariants;
            this.activeVariants = activeVariants;
        }
        
        public long getTotalVariants() {
            return totalVariants;
        }
        
        public long getActiveVariants() {
            return activeVariants;
        }
    }
}
