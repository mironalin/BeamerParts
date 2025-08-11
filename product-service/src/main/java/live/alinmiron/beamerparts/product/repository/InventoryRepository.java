package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Inventory repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // Uses idx_inventory_product index
    List<Inventory> findByProduct(Product product);
    
    // Find by product ID
    List<Inventory> findByProductId(Long productId);
    
    // Find by product and variant
    Optional<Inventory> findByProductAndVariant(Product product, ProductVariant variant);
    
    // Find by product and variant IDs
    Optional<Inventory> findByProductIdAndVariantId(Long productId, Long variantId);
    
    // Find inventory for product without variant (base product)
    Optional<Inventory> findByProductAndVariantIsNull(Product product);
    
    // Find by product ID without variant
    Optional<Inventory> findByProductIdAndVariantIsNull(Long productId);
    
    // Uses idx_inventory_low_stock index
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    List<Inventory> findLowStockItems();
    
    // Find out of stock items
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable = 0")
    List<Inventory> findOutOfStockItems();
    
    // Find items below minimum stock level
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < i.minimumStockLevel")
    List<Inventory> findBelowMinimumStock();
    
    // Find items with reserved quantity
    @Query("SELECT i FROM Inventory i WHERE i.quantityReserved > 0")
    List<Inventory> findItemsWithReservations();
    
    // Count inventory items by product
    long countByProduct(Product product);
    
    // Find inventory by products (bulk operation)
    @Query("SELECT i FROM Inventory i WHERE i.product.id IN :productIds")
    List<Inventory> findByProductIds(@Param("productIds") List<Long> productIds);
    
    // Find inventory for active products
    @Query("SELECT i FROM Inventory i WHERE i.product.status = 'ACTIVE'")
    List<Inventory> findForActiveProducts();
    
    // Get total available quantity for product (all variants)
    @Query("SELECT COALESCE(SUM(i.quantityAvailable), 0) FROM Inventory i WHERE i.product = :product")
    Integer getTotalAvailableQuantityForProduct(@Param("product") Product product);
    
    // Get total reserved quantity for product (all variants)
    @Query("SELECT COALESCE(SUM(i.quantityReserved), 0) FROM Inventory i WHERE i.product = :product")
    Integer getTotalReservedQuantityForProduct(@Param("product") Product product);
    
    // Find products with inventory issues
    @Query("SELECT DISTINCT i.product FROM Inventory i WHERE " +
           "i.quantityAvailable <= i.reorderPoint OR i.quantityAvailable < i.minimumStockLevel")
    List<Product> findProductsWithInventoryIssues();
    
    // Check if inventory exists for product-variant combination
    boolean existsByProductAndVariant(Product product, ProductVariant variant);
    
    // Check if inventory exists for product without variant
    boolean existsByProductAndVariantIsNull(Product product);
}
