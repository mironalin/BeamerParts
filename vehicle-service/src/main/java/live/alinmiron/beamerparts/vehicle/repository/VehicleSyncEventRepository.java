package live.alinmiron.beamerparts.vehicle.repository;

import live.alinmiron.beamerparts.vehicle.entity.VehicleSyncEvent;
import live.alinmiron.beamerparts.vehicle.entity.VehicleSyncEventStatus;
import live.alinmiron.beamerparts.vehicle.entity.VehicleSyncEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vehicle Sync Event repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface VehicleSyncEventRepository extends JpaRepository<VehicleSyncEvent, Long> {
    
    // Uses idx_vehicle_sync_events_status index
    List<VehicleSyncEvent> findByProcessingStatus(VehicleSyncEventStatus status);
    
    // Uses idx_vehicle_sync_events_published index
    List<VehicleSyncEvent> findByPublishedAtAfter(LocalDateTime dateTime);
    
    // Uses idx_vehicle_sync_events_published index
    List<VehicleSyncEvent> findByPublishedAtBefore(LocalDateTime dateTime);
    
    // Find pending events (for processing)
    @Query("SELECT vse FROM VehicleSyncEvent vse WHERE vse.processingStatus = 'PENDING' " +
           "ORDER BY vse.publishedAt ASC")
    List<VehicleSyncEvent> findPendingEventsOrderByPublishedAt();
    
    // Find events by type
    List<VehicleSyncEvent> findByEventType(VehicleSyncEventType eventType);
    
    // Find events by entity code
    List<VehicleSyncEvent> findByEntityCode(String entityCode);
    
    // Find events by type and status
    @Query("SELECT vse FROM VehicleSyncEvent vse WHERE vse.eventType = :eventType " +
           "AND vse.processingStatus = :status ORDER BY vse.publishedAt DESC")
    List<VehicleSyncEvent> findByEventTypeAndStatus(@Param("eventType") VehicleSyncEventType eventType,
                                                   @Param("status") VehicleSyncEventStatus status);
    
    // Find recent events
    @Query("SELECT vse FROM VehicleSyncEvent vse WHERE vse.publishedAt >= :since " +
           "ORDER BY vse.publishedAt DESC")
    List<VehicleSyncEvent> findRecentEvents(@Param("since") LocalDateTime since);
    
    // Find failed events for retry
    @Query("SELECT vse FROM VehicleSyncEvent vse WHERE vse.processingStatus = 'FAILED' " +
           "AND vse.publishedAt >= :since ORDER BY vse.publishedAt ASC")
    List<VehicleSyncEvent> findFailedEventsForRetry(@Param("since") LocalDateTime since);
    
    // Mark event as processed
    @Modifying
    @Query("UPDATE VehicleSyncEvent vse SET vse.processingStatus = 'PROCESSED' " +
           "WHERE vse.id = :id")
    int markAsProcessed(@Param("id") Long id);
    
    // Mark event as failed
    @Modifying
    @Query("UPDATE VehicleSyncEvent vse SET vse.processingStatus = 'FAILED' " +
           "WHERE vse.id = :id")
    int markAsFailed(@Param("id") Long id);
    
    // Mark events as processing (for batch processing)
    @Modifying
    @Query("UPDATE VehicleSyncEvent vse SET vse.processingStatus = 'PROCESSING' " +
           "WHERE vse.id IN :ids")
    int markAsProcessing(@Param("ids") List<Long> ids);
    
    // Cleanup old events
    @Modifying
    @Query("DELETE FROM VehicleSyncEvent vse WHERE vse.publishedAt < :cutoffDate " +
           "AND vse.processingStatus = 'PROCESSED'")
    int deleteOldProcessedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Count events by status
    long countByProcessingStatus(VehicleSyncEventStatus status);
    
    // Count events by type
    long countByEventType(VehicleSyncEventType eventType);
    
    // Count pending events
    @Query("SELECT COUNT(vse) FROM VehicleSyncEvent vse WHERE vse.processingStatus = 'PENDING'")
    long countPendingEvents();
    
    // Find latest event for entity
    @Query("SELECT vse FROM VehicleSyncEvent vse WHERE vse.entityCode = :entityCode " +
           "ORDER BY vse.publishedAt DESC LIMIT 1")
    VehicleSyncEvent findLatestEventForEntity(@Param("entityCode") String entityCode);
    
    // Statistics: events by status in time range
    @Query("SELECT vse.processingStatus, COUNT(vse) FROM VehicleSyncEvent vse " +
           "WHERE vse.publishedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY vse.processingStatus")
    List<Object[]> getEventStatsByStatus(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
