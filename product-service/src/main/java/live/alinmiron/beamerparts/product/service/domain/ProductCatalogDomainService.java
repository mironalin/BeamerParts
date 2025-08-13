package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
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
 * Domain service that encapsulates all product catalog business logic.
 * 
 * Business Rules:
 * - Product lookup must be efficient and reliable
 * - Product validation ensures data integrity for transactions
 * - Only active products are available for purchase
 * - Bulk operations maintain performance requirements
 * - Product compatibility resolves through BMW generation codes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductCatalogDomainService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Retrieve product by SKU with business validation.
     * 
     * @param sku Product SKU identifier
     * @return Product if found, empty if not found
     */
    public Optional<Product> findProductBySku(String sku) {
        log.debug("Finding product by SKU: {}", sku);
        
        if (sku == null || sku.trim().isEmpty()) {
            log.warn("Invalid SKU provided: {}", sku);
            return Optional.empty();
        }
        
        Optional<Product> productOpt = productRepository.findBySku(sku.trim());
        
        if (productOpt.isPresent()) {
            log.debug("Found product: {} ({})", productOpt.get().getName(), sku);
        } else {
            log.debug("Product not found for SKU: {}", sku);
        }
        
        return productOpt;
    }

    /**
     * Retrieve multiple products by SKUs efficiently.
     * 
     * @param skus List of product SKU identifiers
     * @return List of found products (excludes non-existent ones)
     */
    public List<Product> findProductsBulk(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            log.warn("Empty SKU list provided for bulk lookup");
            return List.of();
        }
        
        log.debug("Finding {} products in bulk", skus.size());
        
        // Business rule: Filter out invalid SKUs before database query
        List<String> validSkus = skus.stream()
                .filter(sku -> sku != null && !sku.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
        
        if (validSkus.isEmpty()) {
            log.warn("No valid SKUs found in bulk request");
            return List.of();
        }
        
        List<Product> products = productRepository.findBySkus(validSkus);
        log.debug("Found {} products out of {} requested SKUs", products.size(), validSkus.size());
        
        return products;
    }

    /**
     * Validate if product exists and is available for purchase.
     * 
     * @param sku Product SKU identifier
     * @return true if product exists and is active
     */
    public boolean isProductAvailableForPurchase(String sku) {
        log.debug("Validating product availability for SKU: {}", sku);
        
        return findProductBySku(sku)
                .map(Product::isAvailableForPurchase)
                .orElse(false);
    }

    /**
     * Validate product with detailed business rules for cart/order operations.
     * 
     * @param sku Product SKU identifier
     * @return ProductValidationResult with detailed validation information
     */
    public ProductValidationResult validateProductForTransaction(String sku) {
        log.debug("Validating product for transaction: {}", sku);
        
        if (sku == null || sku.trim().isEmpty()) {
            return ProductValidationResult.invalid("SKU cannot be empty");
        }
        
        Optional<Product> productOpt = findProductBySku(sku);
        
        if (productOpt.isEmpty()) {
            return ProductValidationResult.notFound(sku);
        }
        
        Product product = productOpt.get();
        
        // Business rule: Only active products can be added to cart/orders
        if (!product.isAvailableForPurchase()) {
            return ProductValidationResult.unavailable(product, "Product is not active");
        }
        
        // Business rule: Product must have valid pricing
        if (product.getBasePrice() == null || product.getBasePrice().signum() <= 0) {
            return ProductValidationResult.invalid(product, "Product has invalid pricing");
        }
        
        return ProductValidationResult.valid(product);
    }

    /**
     * Find products compatible with specific BMW generation.
     * 
     * @param generationCode BMW generation code (e.g., "F30", "E90")
     * @return List of compatible products
     */
    public List<Product> findProductsByBmwGeneration(String generationCode) {
        log.debug("Finding products compatible with BMW generation: {}", generationCode);
        
        if (generationCode == null || generationCode.trim().isEmpty()) {
            log.warn("Invalid generation code provided: {}", generationCode);
            return List.of();
        }
        
        List<Product> products = productRepository.findByCompatibleGeneration(generationCode.trim());
        
        // Business rule: Only return active products for customer purchase
        List<Product> activeProducts = products.stream()
                .filter(Product::isAvailableForPurchase)
                .toList();
        
        log.debug("Found {} active products compatible with generation {}", activeProducts.size(), generationCode);
        
        return activeProducts;
    }

    /**
     * Check if product exists (regardless of status).
     * 
     * @param sku Product SKU identifier
     * @return true if product exists in catalog
     */
    public boolean productExists(String sku) {
        return findProductBySku(sku).isPresent();
    }

    /**
     * Get product display information for UI purposes.
     * 
     * @param sku Product SKU identifier
     * @return ProductDisplayInfo if product exists
     */
    public Optional<ProductDisplayInfo> getProductDisplayInfo(String sku) {
        return findProductBySku(sku)
                .map(product -> ProductDisplayInfo.builder()
                        .sku(product.getSku())
                        .name(product.getName())
                        .displayName(product.getDisplayName())
                        .basePrice(product.getBasePrice())
                        .brand(product.getBrand())
                        .categoryName(product.getCategory().getName())
                        .isAvailable(product.isAvailableForPurchase())
                        .isFeatured(product.getIsFeatured())
                        .build());
    }

    /**
     * Create a new product with business validation
     * 
     * Business Rules:
     * - SKU must be unique across all products
     * - Slug must be unique across all products  
     * - Category must exist and be active
     * - Product inherits business rules from category
     */
    @Transactional
    public Product createProduct(String name, String slug, String sku, String description, 
                               String shortDescription, BigDecimal basePrice, Long categoryId,
                               String brand, Integer weightGrams, String dimensionsJson, 
                               Boolean isFeatured, ProductStatus status) {
        log.debug("Creating new product with SKU: {}", sku);
        
        // Business Rule: Validate SKU uniqueness
        if (productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException("Product with SKU '" + sku + "' already exists");
        }
        
        // Business Rule: Validate slug uniqueness
        if (productRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Product with slug '" + slug + "' already exists");
        }
        
        // Business Rule: Validate category exists and is active
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        if (!category.getIsActive()) {
            throw new IllegalArgumentException("Cannot create product in inactive category");
        }
        
        // Create product entity
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .sku(sku)
                .description(description)
                .shortDescription(shortDescription)
                .basePrice(basePrice)
                .category(category)
                .brand(brand)
                .weightGrams(weightGrams)
                .dimensionsJson(dimensionsJson)
                .isFeatured(isFeatured)
                .status(status)
                .build();
        
        product = productRepository.save(product);
        log.info("Created new product: {} with SKU: {} and ID: {}", product.getName(), product.getSku(), product.getId());
        
        return product;
    }

    /**
     * Update product with business validation
     * 
     * Business Rules:
     * - SKU must remain unique if changed
     * - Slug must remain unique if changed
     * - New category must exist and be active
     * - Cannot move product to inactive category
     */
    @Transactional
    public Product updateProduct(Long id, String name, String slug, String sku, String description,
                               String shortDescription, BigDecimal basePrice, Long categoryId,
                               String brand, Integer weightGrams, String dimensionsJson,
                               Boolean isFeatured, ProductStatus status) {
        log.debug("Updating product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        
        // Business Rule: Validate SKU uniqueness if changed
        if (!product.getSku().equals(sku) && 
            productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new IllegalArgumentException("Product with SKU '" + sku + "' already exists");
        }
        
        // Business Rule: Validate slug uniqueness if changed
        if (!product.getSlug().equals(slug) && 
            productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("Product with slug '" + slug + "' already exists");
        }
        
        // Business Rule: Validate category if changed
        if (!product.getCategory().getId().equals(categoryId)) {
            Category newCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            
            if (!newCategory.getIsActive()) {
                throw new IllegalArgumentException("Cannot move product to inactive category");
            }
            
            product.setCategory(newCategory);
        }
        
        // Update fields
        product.setName(name);
        product.setSlug(slug);
        product.setSku(sku);
        product.setDescription(description);
        product.setShortDescription(shortDescription);
        product.setBasePrice(basePrice);
        product.setBrand(brand);
        product.setWeightGrams(weightGrams);
        product.setDimensionsJson(dimensionsJson);
        product.setIsFeatured(isFeatured);
        product.setStatus(status);
        
        product = productRepository.save(product);
        log.info("Updated product: {} with SKU: {} and ID: {}", product.getName(), product.getSku(), product.getId());
        
        return product;
    }

    /**
     * Delete product (soft delete by changing status)
     * 
     * Business Rule: Products are soft deleted to preserve order history
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Deleting product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
        
        log.info("Deleted (discontinued) product: {} with SKU: {} and ID: {}", 
                product.getName(), product.getSku(), product.getId());
    }

    /**
     * Delete product by SKU (soft delete)
     */
    @Transactional
    public boolean deleteProductBySku(String sku) {
        log.debug("Deleting product with SKU: {}", sku);
        
        Optional<Product> productOpt = productRepository.findBySku(sku);
        if (productOpt.isEmpty()) {
            return false;
        }
        
        Product product = productOpt.get();
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        
        log.info("Product {} soft deleted (set to INACTIVE)", sku);
        return true;
    }

    /**
     * Get all products with pagination
     */
    public Page<Product> findAllProducts(Pageable pageable) {
        log.debug("Finding all products with pagination");
        return productRepository.findAll(pageable);
    }

    /**
     * Search products with pagination
     */
    public Page<Product> searchProducts(String searchTerm, Pageable pageable) {
        log.debug("Searching products with term: {} (paginated)", searchTerm);
        return productRepository.findBySearchTerm(searchTerm, ProductStatus.ACTIVE.name(), pageable);
    }

    /**
     * Find products by category with pagination
     */
    public Page<Product> findProductsByCategory(Long categoryId, Pageable pageable) {
        log.debug("Finding products for category ID: {} with pagination", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        return productRepository.findByCategoryAndStatus(category, ProductStatus.ACTIVE, pageable);
    }

    /**
     * Get product statistics
     */
    public ProductStats getProductStatistics() {
        long totalProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        long featuredProducts = productRepository.findByIsFeaturedAndStatus(true, ProductStatus.ACTIVE).size();
        long lowStockProducts = productRepository.findProductsWithLowStock().size();
        
        return new ProductStats(totalProducts, featuredProducts, lowStockProducts);
    }

    /**
     * Domain class for product validation results
     */
    public static class ProductValidationResult {
        private final boolean isValid;
        private final boolean exists;
        private final Product product;
        private final String errorMessage;

        private ProductValidationResult(boolean isValid, boolean exists, Product product, String errorMessage) {
            this.isValid = isValid;
            this.exists = exists;
            this.product = product;
            this.errorMessage = errorMessage;
        }

        public static ProductValidationResult valid(Product product) {
            return new ProductValidationResult(true, true, product, null);
        }

        public static ProductValidationResult notFound(String sku) {
            return new ProductValidationResult(false, false, null, "Product not found: " + sku);
        }

        public static ProductValidationResult unavailable(Product product, String reason) {
            return new ProductValidationResult(false, true, product, reason);
        }

        public static ProductValidationResult invalid(String reason) {
            return new ProductValidationResult(false, false, null, reason);
        }

        public static ProductValidationResult invalid(Product product, String reason) {
            return new ProductValidationResult(false, true, product, reason);
        }

        // Getters
        public boolean isValid() { return isValid; }
        public boolean exists() { return exists; }
        public Product getProduct() { return product; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Domain class for product display information
     */
    public static class ProductDisplayInfo {
        private final String sku;
        private final String name;
        private final String displayName;
        private final java.math.BigDecimal basePrice;
        private final String brand;
        private final String categoryName;
        private final boolean isAvailable;
        private final boolean isFeatured;

        private ProductDisplayInfo(String sku, String name, String displayName, 
                                  java.math.BigDecimal basePrice, String brand, 
                                  String categoryName, boolean isAvailable, boolean isFeatured) {
            this.sku = sku;
            this.name = name;
            this.displayName = displayName;
            this.basePrice = basePrice;
            this.brand = brand;
            this.categoryName = categoryName;
            this.isAvailable = isAvailable;
            this.isFeatured = isFeatured;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String sku;
            private String name;
            private String displayName;
            private java.math.BigDecimal basePrice;
            private String brand;
            private String categoryName;
            private boolean isAvailable;
            private boolean isFeatured;

            public Builder sku(String sku) { this.sku = sku; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder displayName(String displayName) { this.displayName = displayName; return this; }
            public Builder basePrice(java.math.BigDecimal basePrice) { this.basePrice = basePrice; return this; }
            public Builder brand(String brand) { this.brand = brand; return this; }
            public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
            public Builder isAvailable(boolean isAvailable) { this.isAvailable = isAvailable; return this; }
            public Builder isFeatured(boolean isFeatured) { this.isFeatured = isFeatured; return this; }

            public ProductDisplayInfo build() {
                return new ProductDisplayInfo(sku, name, displayName, basePrice, brand, 
                                            categoryName, isAvailable, isFeatured);
            }
        }

        // Getters
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public java.math.BigDecimal getBasePrice() { return basePrice; }
        public String getBrand() { return brand; }
        public String getCategoryName() { return categoryName; }
        public boolean isAvailable() { return isAvailable; }
        public boolean isFeatured() { return isFeatured; }
    }

    /**
     * Value object for product statistics
     */
    public static class ProductStats {
        private final Long totalActiveProducts;
        private final Long featuredProducts;
        private final Long lowStockProducts;
        
        public ProductStats(Long totalActiveProducts, Long featuredProducts, Long lowStockProducts) {
            this.totalActiveProducts = totalActiveProducts;
            this.featuredProducts = featuredProducts;
            this.lowStockProducts = lowStockProducts;
        }
        
        public Long getTotalActiveProducts() {
            return totalActiveProducts;
        }
        
        public Long getFeaturedProducts() {
            return featuredProducts;
        }
        
        public Long getLowStockProducts() {
            return lowStockProducts;
        }
    }
}
