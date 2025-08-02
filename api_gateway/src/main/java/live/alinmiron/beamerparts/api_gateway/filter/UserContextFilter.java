package live.alinmiron.beamerparts.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Injects user ID from JWT authentication into the request path.
 * This enables proper user-scoped routing in enterprise applications.
 */
@Component
public class UserContextFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Get user ID from exchange attributes (set by JwtAuthenticationFilter)
        String userId = exchange.getAttribute("userId");
        
        if (userId == null) {
            // This should not happen if JwtAuthenticationFilter ran first
            System.out.println("ERROR: No user ID in exchange attributes");
            userId = "unknown-user";
        }
        
        // Get the current request URI
        URI currentUri = exchange.getRequest().getURI();
        String path = currentUri.getPath();
        
        // Replace {{USER_ID}} placeholder with actual user ID
        String newPath = path.replace("{{USER_ID}}", userId);
        
        // Create new URI with updated path
        URI newUri;
        try {
            newUri = new URI(
                currentUri.getScheme(),
                currentUri.getAuthority(),
                newPath,
                currentUri.getQuery(),
                currentUri.getFragment()
            );
        } catch (Exception e) {
            System.out.println("ERROR: Failed to create new URI with user context - " + e.getMessage());
            return chain.filter(exchange);
        }
        
        // Create modified exchange with new URI
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .uri(newUri)
                .header("X-Original-Path", path)
                .header("X-User-Context", userId)
                .build())
            .build();
        
        System.out.println("INFO: User context filter - Original path: " + path + ", New path: " + newPath + ", User ID: " + userId);
        
        return chain.filter(modifiedExchange);
    }
}
