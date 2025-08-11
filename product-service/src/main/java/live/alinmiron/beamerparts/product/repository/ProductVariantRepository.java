package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductVariant repository for database operations
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    // Find variants by product
    List<ProductVariant> findByProduct(Product product);
    
    // Find variants by product ID
    List<ProductVariant> findByProductId(Long productId);
    
    // Find active variants by product
    List<ProductVariant> findByProductAndIsActive(Product product, Boolean isActive);
    
    // Find active variants by product ID
    List<ProductVariant> findByProductIdAndIsActive(Long productId, Boolean isActive);
    
    // Find by product and SKU suffix
    Optional<ProductVariant> findByProductAndSkuSuffix(Product product, String skuSuffix);
    
    // Find by product ID and SKU suffix
    Optional<ProductVariant> findByProductIdAndSkuSuffix(Long productId, String skuSuffix);
    
    // Find by name
    List<ProductVariant> findByNameContainingIgnoreCase(String name);
    
    // Count variants by product
    long countByProduct(Product product);
    
    // Count active variants by product
    long countByProductAndIsActive(Product product, Boolean isActive);
    
    // Check if SKU suffix exists for product (excluding current variant)
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END " +
           "FROM ProductVariant pv WHERE pv.product = :product AND pv.skuSuffix = :skuSuffix AND pv.id != :id")
    boolean existsByProductAndSkuSuffixAndIdNot(@Param("product") Product product, 
                                               @Param("skuSuffix") String skuSuffix, 
                                               @Param("id") Long id);
    
    // Check if SKU suffix exists for product
    boolean existsByProductAndSkuSuffix(Product product, String skuSuffix);
    
    // Find variants by products (bulk operation)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id IN :productIds AND pv.isActive = true")
    List<ProductVariant> findActiveByProductIds(@Param("productIds") List<Long> productIds);
    
    // Find variants with inventory
    @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
           "JOIN pv.inventory i WHERE i.quantityAvailable > 0 " +
           "AND pv.isActive = true ORDER BY pv.product.name, pv.name")
    List<ProductVariant> findVariantsWithStock();
    
    // Find variants with low stock
    @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
           "JOIN pv.inventory i WHERE i.quantityAvailable <= i.reorderPoint " +
           "AND pv.isActive = true ORDER BY pv.product.name, pv.name")
    List<ProductVariant> findVariantsWithLowStock();
}
