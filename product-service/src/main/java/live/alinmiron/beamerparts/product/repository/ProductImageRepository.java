package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductImage repository for database operations
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    // Find images by product
    List<ProductImage> findByProductOrderBySortOrder(Product product);
    
    // Find images by product ID
    List<ProductImage> findByProductIdOrderBySortOrder(Long productId);
    
    // Find primary image for product
    Optional<ProductImage> findByProductAndIsPrimary(Product product, Boolean isPrimary);
    
    // Find primary image by product ID
    Optional<ProductImage> findByProductIdAndIsPrimary(Long productId, Boolean isPrimary);
    
    // Count images for product
    long countByProduct(Product product);
    
    // Count images by product ID
    long countByProductId(Long productId);
    
    // Find by image URL
    Optional<ProductImage> findByImageUrl(String imageUrl);
    
    // Check if image URL exists
    boolean existsByImageUrl(String imageUrl);
    
    // Find images by products (bulk operation)
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds ORDER BY pi.product.id, pi.sortOrder")
    List<ProductImage> findByProductIds(@Param("productIds") List<Long> productIds);
    
    // Find products without images
    @Query("SELECT p FROM Product p WHERE p.id NOT IN (SELECT DISTINCT pi.product.id FROM ProductImage pi)")
    List<Product> findProductsWithoutImages();
    
    // Update sort order for product images
    @Query("UPDATE ProductImage pi SET pi.sortOrder = pi.sortOrder + 1 " +
           "WHERE pi.product = :product AND pi.sortOrder >= :fromOrder")
    void incrementSortOrderFrom(@Param("product") Product product, @Param("fromOrder") Integer fromOrder);
}
