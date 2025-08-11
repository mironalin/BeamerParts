package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductVariant;
import live.alinmiron.beamerparts.product.entity.StockMovement;
import live.alinmiron.beamerparts.product.entity.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StockMovement repository for database operations
 * Provides audit trail and stock movement tracking
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    // Find movements by product
    List<StockMovement> findByProductOrderByCreatedAtDesc(Product product);
    
    // Find movements by product ID
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    // Find movements by product and variant
    List<StockMovement> findByProductAndVariantOrderByCreatedAtDesc(Product product, ProductVariant variant);
    
    // Find movements by product and variant IDs
    List<StockMovement> findByProductIdAndVariantIdOrderByCreatedAtDesc(Long productId, Long variantId);
    
    // Find movements by type
    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovementType movementType);
    
    // Find movements by user
    List<StockMovement> findByUserCodeOrderByCreatedAtDesc(String userCode);
    
    // Find movements by reference ID
    List<StockMovement> findByReferenceId(String referenceId);
    
    // Find movements within date range
    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    
    // Find movements with pagination
    Page<StockMovement> findByProductOrderByCreatedAtDesc(Product product, Pageable pageable);
    
    // Find movements by type with pagination
    Page<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovementType movementType, Pageable pageable);
    
    // Find recent movements
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    Page<StockMovement> findRecentMovements(Pageable pageable);
    
    // Calculate total quantity change for product
    @Query("SELECT COALESCE(SUM(sm.quantityChange), 0) FROM StockMovement sm " +
           "WHERE sm.product = :product AND sm.movementType = :movementType")
    Integer getTotalQuantityChangeByType(@Param("product") Product product, 
                                       @Param("movementType") StockMovementType movementType);
    
    // Calculate total quantity change for product and variant
    @Query("SELECT COALESCE(SUM(sm.quantityChange), 0) FROM StockMovement sm " +
           "WHERE sm.product = :product AND sm.variant = :variant AND sm.movementType = :movementType")
    Integer getTotalQuantityChangeByTypeAndVariant(@Param("product") Product product, 
                                                 @Param("variant") ProductVariant variant,
                                                 @Param("movementType") StockMovementType movementType);
    
    // Find movements by products (bulk operation)
    @Query("SELECT sm FROM StockMovement sm WHERE sm.product.id IN :productIds ORDER BY sm.createdAt DESC")
    List<StockMovement> findByProductIds(@Param("productIds") List<Long> productIds);
    
    // Count movements by type
    long countByMovementType(StockMovementType movementType);
    
    // Count movements by user
    long countByUserCode(String userCode);
    
    // Find movements for active products
    @Query("SELECT sm FROM StockMovement sm WHERE sm.product.status = 'ACTIVE' ORDER BY sm.createdAt DESC")
    List<StockMovement> findForActiveProducts();
    
    // Find movements with quantity above threshold
    @Query("SELECT sm FROM StockMovement sm WHERE ABS(sm.quantityChange) >= :threshold ORDER BY sm.createdAt DESC")
    List<StockMovement> findLargeMovements(@Param("threshold") Integer threshold);
    
    // Find movements by reason
    List<StockMovement> findByReasonContainingIgnoreCaseOrderByCreatedAtDesc(String reason);
    
    // Get movement statistics for date range
    @Query("SELECT sm.movementType, COUNT(sm), SUM(ABS(sm.quantityChange)) " +
           "FROM StockMovement sm WHERE sm.createdAt BETWEEN :start AND :end " +
           "GROUP BY sm.movementType")
    List<Object[]> getMovementStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
