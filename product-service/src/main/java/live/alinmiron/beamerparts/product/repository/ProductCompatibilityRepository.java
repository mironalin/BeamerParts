package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductCompatibility repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface ProductCompatibilityRepository extends JpaRepository<ProductCompatibility, Long> {
    
    // Uses idx_product_compatibility_product index
    List<ProductCompatibility> findByProduct(Product product);
    
    // Find by product ID
    List<ProductCompatibility> findByProductId(Long productId);
    
    // Uses idx_product_compatibility_generation index
    List<ProductCompatibility> findByGenerationCache(BmwGenerationCache generationCache);
    
    // Find by generation code
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.generationCache.code = :generationCode")
    List<ProductCompatibility> findByGenerationCode(@Param("generationCode") String generationCode);
    
    // Find by product and generation
    Optional<ProductCompatibility> findByProductAndGenerationCache(Product product, BmwGenerationCache generationCache);
    
    // Find by product ID and generation code
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.product.id = :productId AND pc.generationCache.code = :generationCode")
    Optional<ProductCompatibility> findByProductIdAndGenerationCode(@Param("productId") Long productId, 
                                                                   @Param("generationCode") String generationCode);
    
    // Find verified compatibility entries
    List<ProductCompatibility> findByIsVerified(Boolean isVerified);
    
    // Find verified compatibility for product
    List<ProductCompatibility> findByProductAndIsVerified(Product product, Boolean isVerified);
    
    // Find compatibility for series
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.generationCache.seriesCache.code = :seriesCode")
    List<ProductCompatibility> findBySeriesCode(@Param("seriesCode") String seriesCode);
    
    // Find products compatible with generation
    @Query("SELECT DISTINCT pc.product FROM ProductCompatibility pc " +
           "WHERE pc.generationCache.code = :generationCode AND pc.product.status = 'ACTIVE'")
    List<Product> findProductsByGenerationCode(@Param("generationCode") String generationCode);
    
    // Find products compatible with series
    @Query("SELECT DISTINCT pc.product FROM ProductCompatibility pc " +
           "WHERE pc.generationCache.seriesCache.code = :seriesCode AND pc.product.status = 'ACTIVE'")
    List<Product> findProductsBySeriesCode(@Param("seriesCode") String seriesCode);
    
    // Find verified products compatible with generation
    @Query("SELECT DISTINCT pc.product FROM ProductCompatibility pc " +
           "WHERE pc.generationCache.code = :generationCode AND pc.isVerified = true AND pc.product.status = 'ACTIVE'")
    List<Product> findVerifiedProductsByGenerationCode(@Param("generationCode") String generationCode);
    
    // Count compatibility entries by product
    long countByProduct(Product product);
    
    // Count compatibility entries by generation
    long countByGenerationCache(BmwGenerationCache generationCache);
    
    // Count verified compatibility entries
    long countByIsVerified(Boolean isVerified);
    
    // Check if compatibility exists
    boolean existsByProductAndGenerationCache(Product product, BmwGenerationCache generationCache);
    
    // Find compatibility by products (bulk operation)
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.product.id IN :productIds")
    List<ProductCompatibility> findByProductIds(@Param("productIds") List<Long> productIds);
    
    // Find compatibility by generation codes (bulk operation)
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.generationCache.code IN :generationCodes")
    List<ProductCompatibility> findByGenerationCodes(@Param("generationCodes") List<String> generationCodes);
    
    // Find products with most compatibility
    @Query("SELECT pc.product, COUNT(pc) as compatibilityCount FROM ProductCompatibility pc " +
           "WHERE pc.product.status = 'ACTIVE' " +
           "GROUP BY pc.product " +
           "ORDER BY compatibilityCount DESC")
    List<Object[]> findProductsWithMostCompatibility();
    
    // Find generations with most compatible products
    @Query("SELECT pc.generationCache, COUNT(pc) as productCount FROM ProductCompatibility pc " +
           "WHERE pc.product.status = 'ACTIVE' " +
           "GROUP BY pc.generationCache " +
           "ORDER BY productCount DESC")
    List<Object[]> findGenerationsWithMostProducts();
    
    // Find compatibility entries needing verification
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.isVerified = false " +
           "AND pc.product.status = 'ACTIVE' ORDER BY pc.createdAt DESC")
    List<ProductCompatibility> findUnverifiedCompatibility();
    
    // Find compatibility by category
    @Query("SELECT pc FROM ProductCompatibility pc WHERE pc.product.category.id = :categoryId")
    List<ProductCompatibility> findByCategoryId(@Param("categoryId") Long categoryId);
}
