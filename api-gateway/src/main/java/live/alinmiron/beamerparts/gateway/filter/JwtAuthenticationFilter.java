package live.alinmiron.beamerparts.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extracts user ID from JWT token and adds it to the request context.
 */
@Component
public class JwtAuthenticationFilter implements GatewayFilter {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // For development/testing: allow requests without auth but log warning
            System.out.println("WARNING: Request without authentication - using default user ID for development");
            exchange.getAttributes().put("userId", "dev-user-123");
            return chain.filter(exchange);
        }
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String userId = extractUserIdFromToken(token);
            
            if (userId == null) {
                // In production, this would return 401 Unauthorized
                // For development, we'll use a default user ID
                System.out.println("WARNING: Invalid token - using default user ID for development");
                userId = "dev-user-123";
            }
            
            // Store user ID in exchange attributes for later use
            exchange.getAttributes().put("userId", userId);
            
            // Add user ID to headers for downstream services
            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-ID", userId)
                    .build())
                .build();
                
            return chain.filter(modifiedExchange);
            
        } catch (Exception e) {
            System.out.println("ERROR: Failed to process JWT token - " + e.getMessage());
            // In production, return 401 Unauthorized
            // For development, continue with default user
            exchange.getAttributes().put("userId", "dev-user-123");
            return chain.filter(exchange);
        }
    }
    
    /**
     * Extract user ID from JWT token payload
     * In a real enterprise application, this would:
     * 1. Verify token signature using public key
     * 2. Check token expiration
     * 3. Validate issuer and audience claims
     * 4. Extract user claims from payload
     */
    private String extractUserIdFromToken(String token) {
        try {
            // Split JWT token (header.payload.signature)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode payload (base64)
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            
            // Parse JSON payload
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            
            // Extract user ID from claims
            // Common claim names: "sub", "userId", "user_id", "id"
            Object userId = claims.get("sub");
            if (userId == null) {
                userId = claims.get("userId");
            }
            if (userId == null) {
                userId = claims.get("user_id");
            }
            if (userId == null) {
                userId = claims.get("id");
            }
            
            return userId != null ? userId.toString() : null;
            
        } catch (Exception e) {
            System.out.println("ERROR: Failed to decode JWT payload - " + e.getMessage());
            return null;
        }
    }
}
