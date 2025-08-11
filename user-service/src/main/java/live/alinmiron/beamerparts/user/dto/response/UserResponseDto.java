package live.alinmiron.beamerparts.user.dto.response;

import live.alinmiron.beamerparts.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User DTO for API responses
 * Excludes sensitive information like password hash
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String fullName;
    private Boolean isAdmin;
    private Boolean isSuperAdmin;
    
    // Statistics (optional, for admin views)
    private Integer vehicleCount;
    private Integer cartItemCount;
}
