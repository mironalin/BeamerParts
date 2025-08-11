package live.alinmiron.beamerparts.user.repository;

import live.alinmiron.beamerparts.user.entity.User;
import live.alinmiron.beamerparts.user.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User vehicle repository for BMW vehicle preferences
 * Leverages database indexes for optimal performance
 */
@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
    
    // Uses idx_user_vehicles_user index
    List<UserVehicle> findByUser(User user);
    
    // Uses idx_user_vehicles_user index
    List<UserVehicle> findByUserId(Long userId);
    
    // Find primary vehicle for user
    @Query("SELECT uv FROM UserVehicle uv WHERE uv.user = :user AND uv.isPrimary = true")
    Optional<UserVehicle> findPrimaryVehicleByUser(@Param("user") User user);
    
    // Find primary vehicle by user ID
    @Query("SELECT uv FROM UserVehicle uv WHERE uv.user.id = :userId AND uv.isPrimary = true")
    Optional<UserVehicle> findPrimaryVehicleByUserId(@Param("userId") Long userId);
    
    // Uses idx_user_vehicles_series index
    List<UserVehicle> findBySeriesCode(String seriesCode);
    
    // Uses idx_user_vehicles_generation index
    List<UserVehicle> findByGenerationCode(String generationCode);
    
    // Find vehicles by series and generation
    @Query("SELECT uv FROM UserVehicle uv WHERE uv.seriesCode = :seriesCode " +
           "AND uv.generationCode = :generationCode")
    List<UserVehicle> findBySeriesAndGeneration(@Param("seriesCode") String seriesCode, 
                                               @Param("generationCode") String generationCode);
    
    // Find users with specific BMW models (for marketing/analytics)
    @Query("SELECT DISTINCT uv.user FROM UserVehicle uv WHERE uv.seriesCode = :seriesCode " +
           "AND uv.generationCode = :generationCode")
    List<User> findUsersBySeriesAndGeneration(@Param("seriesCode") String seriesCode,
                                             @Param("generationCode") String generationCode);
    
    // Check if user has a specific vehicle combination
    @Query("SELECT CASE WHEN COUNT(uv) > 0 THEN true ELSE false END " +
           "FROM UserVehicle uv WHERE uv.user = :user " +
           "AND uv.seriesCode = :seriesCode AND uv.generationCode = :generationCode")
    boolean userHasVehicle(@Param("user") User user, 
                          @Param("seriesCode") String seriesCode, 
                          @Param("generationCode") String generationCode);
    
    // Set all vehicles as non-primary for a user (before setting a new primary)
    @Modifying
    @Query("UPDATE UserVehicle uv SET uv.isPrimary = false WHERE uv.user = :user")
    int clearPrimaryVehicleForUser(@Param("user") User user);
    
    // Count vehicles per series (analytics)
    @Query("SELECT uv.seriesCode, COUNT(uv) FROM UserVehicle uv GROUP BY uv.seriesCode")
    List<Object[]> countVehiclesBySeries();
    
    // Count vehicles per generation (analytics)
    @Query("SELECT uv.generationCode, COUNT(uv) FROM UserVehicle uv GROUP BY uv.generationCode")
    List<Object[]> countVehiclesByGeneration();
    
    // Find recent vehicle additions
    @Query("SELECT uv FROM UserVehicle uv WHERE uv.createdAt >= :cutoffDate")
    List<UserVehicle> findVehiclesAddedInLastDays(@Param("cutoffDate") LocalDateTime cutoffDate);
}
