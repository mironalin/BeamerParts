package live.alinmiron.beamerparts.user.repository;

import live.alinmiron.beamerparts.user.entity.CartItem;
import live.alinmiron.beamerparts.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cart item repository for shopping cart functionality
 * Leverages database indexes for optimal performance
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // Uses idx_cart_items_user index
    List<CartItem> findByUser(User user);
    
    // Uses idx_cart_items_user index
    List<CartItem> findByUserId(Long userId);
    
    // Uses idx_cart_items_product index
    List<CartItem> findByProductSku(String productSku);
    
    // Find specific cart item for user and product
    @Query("SELECT ci FROM CartItem ci WHERE ci.user = :user " +
           "AND ci.productSku = :productSku " +
           "AND (:variantSku IS NULL AND ci.variantSku IS NULL OR ci.variantSku = :variantSku)")
    Optional<CartItem> findByUserAndProduct(@Param("user") User user,
                                           @Param("productSku") String productSku,
                                           @Param("variantSku") String variantSku);
    
    // Find cart item by user ID and product
    @Query("SELECT ci FROM CartItem ci WHERE ci.user.id = :userId " +
           "AND ci.productSku = :productSku " +
           "AND (:variantSku IS NULL AND ci.variantSku IS NULL OR ci.variantSku = :variantSku)")
    Optional<CartItem> findByUserIdAndProduct(@Param("userId") Long userId,
                                             @Param("productSku") String productSku,
                                             @Param("variantSku") String variantSku);
    
    // Calculate total cart value for user
    @Query("SELECT COALESCE(SUM(ci.unitPrice * ci.quantity), 0) FROM CartItem ci WHERE ci.user = :user")
    BigDecimal calculateTotalCartValue(@Param("user") User user);
    
    // Calculate total cart value by user ID
    @Query("SELECT COALESCE(SUM(ci.unitPrice * ci.quantity), 0) FROM CartItem ci WHERE ci.user.id = :userId")
    BigDecimal calculateTotalCartValueByUserId(@Param("userId") Long userId);
    
    // Count items in cart
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.user = :user")
    Long countItemsInCart(@Param("user") User user);
    
    // Count items in cart by user ID
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.user.id = :userId")
    Long countItemsInCartByUserId(@Param("userId") Long userId);
    
    // Clear entire cart for user
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.user = :user")
    int clearCartForUser(@Param("user") User user);
    
    // Clear cart by user ID
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.user.id = :userId")
    int clearCartForUserId(@Param("userId") Long userId);
    
    // Remove specific product from all carts (when product is discontinued)
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.productSku = :productSku")
    int removeProductFromAllCarts(@Param("productSku") String productSku);
    
    // Uses idx_cart_items_created index - find old cart items for cleanup
    List<CartItem> findByCreatedAtBefore(LocalDateTime dateTime);
    
    // Delete old cart items (cleanup job)
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.createdAt < :dateTime")
    int deleteOldCartItems(@Param("dateTime") LocalDateTime dateTime);
    
    // Find popular products (analytics)
    @Query("SELECT ci.productSku, COUNT(ci), SUM(ci.quantity) FROM CartItem ci " +
           "GROUP BY ci.productSku ORDER BY COUNT(ci) DESC")
    List<Object[]> findPopularProducts();
    
    // Find carts with specific products (for targeted marketing)
    @Query("SELECT DISTINCT ci.user FROM CartItem ci WHERE ci.productSku IN :productSkus")
    List<User> findUsersWithProductsInCart(@Param("productSkus") List<String> productSkus);
    
    // Check if user has specific product in cart
    @Query("SELECT CASE WHEN COUNT(ci) > 0 THEN true ELSE false END " +
           "FROM CartItem ci WHERE ci.user = :user AND ci.productSku = :productSku")
    boolean userHasProductInCart(@Param("user") User user, @Param("productSku") String productSku);
}
