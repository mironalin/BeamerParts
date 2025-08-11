package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Uses idx_products_sku index
    Optional<Product> findBySku(String sku);
    
    // Uses idx_products_sku index
    boolean existsBySku(String sku);
    
    // Find by slug (uses unique index)
    Optional<Product> findBySlug(String slug);
    
    // Check if slug exists
    boolean existsBySlug(String slug);
    
    // Uses idx_products_status index
    List<Product> findByStatus(ProductStatus status);
    
    // Uses idx_products_status index
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    // Uses idx_products_category index
    List<Product> findByCategory(Category category);
    
    // Uses idx_products_category index
    Page<Product> findByCategory(Category category, Pageable pageable);
    
    // Uses idx_products_category and idx_products_status indexes
    List<Product> findByCategoryAndStatus(Category category, ProductStatus status);
    
    // Uses idx_products_category and idx_products_status indexes
    Page<Product> findByCategoryAndStatus(Category category, ProductStatus status, Pageable pageable);
    
    // Uses idx_products_featured index
    List<Product> findByIsFeatured(Boolean isFeatured);
    
    // Uses idx_products_featured and idx_products_status indexes
    List<Product> findByIsFeaturedAndStatus(Boolean isFeatured, ProductStatus status);
    
    // Find by brand
    List<Product> findByBrandAndStatus(String brand, ProductStatus status);
    
    // Price range queries
    List<Product> findByBasePriceBetweenAndStatus(BigDecimal minPrice, BigDecimal maxPrice, ProductStatus status);
    
    // Uses idx_products_search index for full-text search
    @Query(value = "SELECT * FROM products p WHERE " +
           "to_tsvector('english', p.name || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :searchTerm) " +
           "AND p.status = :status", nativeQuery = true)
    List<Product> findBySearchTerm(@Param("searchTerm") String searchTerm, @Param("status") String status);
    
    // Full-text search with pagination
    @Query(value = "SELECT * FROM products p WHERE " +
           "to_tsvector('english', p.name || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :searchTerm) " +
           "AND p.status = :status ORDER BY p.name", 
           countQuery = "SELECT COUNT(*) FROM products p WHERE " +
           "to_tsvector('english', p.name || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :searchTerm) " +
           "AND p.status = :status", 
           nativeQuery = true)
    Page<Product> findBySearchTerm(@Param("searchTerm") String searchTerm, @Param("status") String status, Pageable pageable);
    
    // Find by name (case-insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND p.status = :status ORDER BY p.name")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name, @Param("status") ProductStatus status);
    
    // Count products by status
    long countByStatus(ProductStatus status);
    
    // Count products by category
    long countByCategory(Category category);
    
    // Count products by category and status
    long countByCategoryAndStatus(Category category, ProductStatus status);
    
    // Check if SKU exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Product p WHERE p.sku = :sku AND p.id != :id")
    boolean existsBySkuAndIdNot(@Param("sku") String sku, @Param("id") Long id);
    
    // Check if slug exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Product p WHERE p.slug = :slug AND p.id != :id")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);
    
    // Find products by SKUs (bulk operation)
    @Query("SELECT p FROM Product p WHERE p.sku IN :skus")
    List<Product> findBySkus(@Param("skus") List<String> skus);
    
    // Find products with low stock
    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN p.inventory i WHERE i.quantityAvailable <= i.reorderPoint " +
           "AND p.status = 'ACTIVE' ORDER BY p.name")
    List<Product> findProductsWithLowStock();
    
    // Find products by generation compatibility
    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN p.compatibility pc WHERE pc.generationCache.code = :generationCode " +
           "AND p.status = 'ACTIVE' ORDER BY p.name")
    List<Product> findByCompatibleGeneration(@Param("generationCode") String generationCode);
    
    // Find products by series compatibility
    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN p.compatibility pc WHERE pc.generationCache.seriesCache.code = :seriesCode " +
           "AND p.status = 'ACTIVE' ORDER BY p.name")
    List<Product> findByCompatibleSeries(@Param("seriesCode") String seriesCode);
    
    // Find recently updated products
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' " +
           "ORDER BY p.updatedAt DESC")
    Page<Product> findRecentlyUpdated(Pageable pageable);
}
