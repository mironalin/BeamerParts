package live.alinmiron.beamerparts.user.repository;

import live.alinmiron.beamerparts.user.entity.User;
import live.alinmiron.beamerparts.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Uses idx_users_email index
    Optional<User> findByEmail(String email);
    
    // Uses idx_users_email index
    boolean existsByEmail(String email);
    
    // Uses idx_users_role index
    List<User> findByRole(UserRole role);
    
    // Uses idx_users_active index
    List<User> findByIsActive(Boolean isActive);
    
    // Combined query uses both idx_users_role and idx_users_active
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
    
    // Custom query for admin users
    @Query("SELECT u FROM User u WHERE u.role IN ('ADMIN', 'SUPER_ADMIN') AND u.isActive = true")
    List<User> findActiveAdmins();
    
    // Search users by name (case-insensitive)
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND u.isActive = true")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find users who joined recently
    @Query("SELECT u FROM User u WHERE u.createdAt >= :cutoffDate AND u.isActive = true")
    List<User> findUsersJoinedInLastDays(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Count users by role
    long countByRole(UserRole role);
    
    // Count active users
    long countByIsActive(Boolean isActive);
}
