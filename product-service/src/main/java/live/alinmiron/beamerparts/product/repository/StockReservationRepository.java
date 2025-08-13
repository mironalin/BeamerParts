package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stock reservation repository for managing temporary stock holds
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    
    // Find active reservation by ID
    Optional<StockReservation> findByReservationIdAndIsActiveTrue(String reservationId);
    
    // Find active reservations for user
    List<StockReservation> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);
    
    // Find active reservations for product
    @Query("SELECT sr FROM StockReservation sr WHERE sr.product.sku = :sku AND sr.isActive = true")
    List<StockReservation> findActiveReservationsByProductSku(@Param("sku") String sku);
    
    // Find expired reservations
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt < :now AND sr.isActive = true")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);
    
    // Deactivate reservation
    @Modifying
    @Query("UPDATE StockReservation sr SET sr.isActive = false WHERE sr.reservationId = :reservationId")
    int deactivateReservation(@Param("reservationId") String reservationId);
    
    // Deactivate expired reservations
    @Modifying
    @Query("UPDATE StockReservation sr SET sr.isActive = false WHERE sr.expiresAt < :now AND sr.isActive = true")
    int deactivateExpiredReservations(@Param("now") LocalDateTime now);
    
    // Get total reserved quantity for product
    @Query("SELECT COALESCE(SUM(sr.quantityReserved), 0) FROM StockReservation sr " +
           "WHERE sr.product.sku = :sku AND sr.isActive = true")
    Integer getTotalReservedQuantityForProduct(@Param("sku") String sku);
    
    // Check if user has active reservations
    boolean existsByUserIdAndIsActiveTrue(String userId);
}
