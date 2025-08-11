package live.alinmiron.beamerparts.user.repository;

import live.alinmiron.beamerparts.user.entity.User;
import live.alinmiron.beamerparts.user.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User refresh token repository for JWT token management
 * Leverages database indexes for optimal performance
 */
@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    
    // Uses idx_user_refresh_tokens_token_hash (unique index)
    Optional<UserRefreshToken> findByTokenHash(String tokenHash);
    
    // Uses idx_user_refresh_tokens_user index
    List<UserRefreshToken> findByUser(User user);
    
    // Uses idx_user_refresh_tokens_user index
    List<UserRefreshToken> findByUserId(Long userId);
    
    // Find valid tokens for a user
    @Query("SELECT rt FROM UserRefreshToken rt WHERE rt.user = :user " +
           "AND rt.isRevoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    List<UserRefreshToken> findValidTokensByUser(@Param("user") User user);
    
    // Find valid token by hash
    @Query("SELECT rt FROM UserRefreshToken rt WHERE rt.tokenHash = :tokenHash " +
           "AND rt.isRevoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    Optional<UserRefreshToken> findValidTokenByHash(@Param("tokenHash") String tokenHash);
    
    // Uses idx_user_refresh_tokens_expires index
    List<UserRefreshToken> findByExpiresAtBefore(LocalDateTime dateTime);
    
    // Revoke all tokens for a user
    @Modifying
    @Query("UPDATE UserRefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user")
    int revokeAllTokensForUser(@Param("user") User user);
    
    // Revoke all tokens for a user by ID
    @Modifying
    @Query("UPDATE UserRefreshToken rt SET rt.isRevoked = true WHERE rt.user.id = :userId")
    int revokeAllTokensForUserId(@Param("userId") Long userId);
    
    // Delete expired tokens (cleanup job)
    @Modifying
    @Query("DELETE FROM UserRefreshToken rt WHERE rt.expiresAt < :dateTime")
    int deleteExpiredTokens(@Param("dateTime") LocalDateTime dateTime);
    
    // Count valid tokens for a user
    @Query("SELECT COUNT(rt) FROM UserRefreshToken rt WHERE rt.user = :user " +
           "AND rt.isRevoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countValidTokensByUser(@Param("user") User user);
    
    // Check if token exists and is valid
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
           "FROM UserRefreshToken rt WHERE rt.tokenHash = :tokenHash " +
           "AND rt.isRevoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    boolean existsValidToken(@Param("tokenHash") String tokenHash);
}
