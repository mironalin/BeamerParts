package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Internal DTO for stock reservation responses
 * Used in service-to-service communication for inventory operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationDto {
    
    private String reservationId; // Unique identifier for the reservation
    private String productSku;
    private String variantSku; // null for base product
    private Integer quantityReserved;
    private String userId;
    private String orderId; // Optional reference
    private String source; // "cart", "order", "admin"
    
    // Inventory state after reservation
    private Integer remainingStock;
    private Integer totalReserved;
    
    // Reservation details
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
    
    // Success/failure info
    private boolean success;
    private String failureReason; // If success = false
    
    /**
     * Create successful reservation
     */
    public static StockReservationDto success(String reservationId, String productSku, 
                                              String variantSku, Integer quantityReserved, 
                                              String userId, Integer remainingStock,
                                              LocalDateTime expiresAt) {
        return StockReservationDto.builder()
                .reservationId(reservationId)
                .productSku(productSku)
                .variantSku(variantSku)
                .quantityReserved(quantityReserved)
                .userId(userId)
                .remainingStock(remainingStock)
                .reservedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .isActive(true)
                .success(true)
                .build();
    }
    
    /**
     * Create failed reservation
     */
    public static StockReservationDto failure(String productSku, String variantSku, 
                                              String userId, String failureReason) {
        return StockReservationDto.builder()
                .productSku(productSku)
                .variantSku(variantSku)
                .userId(userId)
                .success(false)
                .failureReason(failureReason)
                .isActive(false)
                .build();
    }
}