package live.alinmiron.beamerparts.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import live.alinmiron.beamerparts.gateway.filter.JwtAuthenticationFilter;
import live.alinmiron.beamerparts.gateway.filter.UserContextFilter;

/**
 * This configuration handles proper user context injection and routing
 * for authenticated endpoints that require user identification.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // =============================================================================
            // Authentication Routes (No user context needed)
            // =============================================================================
            .route("auth-register-v1", r -> r
                .path("/api/v1/auth/register")
                .filters(f -> f
                    .rewritePath("/api/v1/auth/register", "/internal/auth/register"))
                .uri("http://localhost:8081"))
                
            .route("auth-login-v1", r -> r
                .path("/api/v1/auth/login")
                .filters(f -> f
                    .rewritePath("/api/v1/auth/login", "/internal/auth/login"))
                .uri("http://localhost:8081"))
                
            .route("auth-refresh-v1", r -> r
                .path("/api/v1/auth/refresh")
                .filters(f -> f
                    .rewritePath("/api/v1/auth/refresh", "/internal/auth/refresh-token"))
                .uri("http://localhost:8081"))
                
            .route("auth-logout-v1", r -> r
                .path("/api/v1/auth/logout")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract user ID from JWT
                    .rewritePath("/api/v1/auth/logout", "/internal/auth/logout"))
                .uri("http://localhost:8081"))
                
            .route("auth-me-v1", r -> r
                .path("/api/v1/auth/me")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract user ID from JWT
                    .rewritePath("/api/v1/auth/me", "/internal/auth/validate-token"))
                .uri("http://localhost:8081"))
                
            // =============================================================================
            // Cart Routes (Require user context from JWT)
            // =============================================================================
            .route("cart-get-v1", r -> r
                .path("/api/v1/cart")
                .and().method("GET")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract userId from JWT
                    .filter(new UserContextFilter()) // Inject userId into path
                    .rewritePath("/api/v1/cart", "/internal/users/{{USER_ID}}/cart"))
                .uri("http://localhost:8081"))
                
            .route("cart-add-item-v1", r -> r
                .path("/api/v1/cart/items")
                .and().method("POST")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract userId from JWT
                    .filter(new UserContextFilter()) // Inject userId into path
                    .rewritePath("/api/v1/cart/items", "/internal/users/{{USER_ID}}/cart/items"))
                .uri("http://localhost:8081"))
                
            .route("cart-update-item-v1", r -> r
                .path("/api/v1/cart/items/{itemId}")
                .and().method("PUT")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract userId from JWT
                    .filter(new UserContextFilter()) // Inject userId into path
                    .rewritePath("/api/v1/cart/items/(?<itemId>.*)", "/internal/users/{{USER_ID}}/cart/items/${itemId}"))
                .uri("http://localhost:8081"))
                
            .route("cart-delete-item-v1", r -> r
                .path("/api/v1/cart/items/{itemId}")
                .and().method("DELETE")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract userId from JWT
                    .filter(new UserContextFilter()) // Inject userId into path
                    .rewritePath("/api/v1/cart/items/(?<itemId>.*)", "/internal/users/{{USER_ID}}/cart/items/${itemId}"))
                .uri("http://localhost:8081"))
                
            .route("cart-clear-v1", r -> r
                .path("/api/v1/cart/clear")
                .and().method("DELETE")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter()) // Extract userId from JWT
                    .filter(new UserContextFilter()) // Inject userId into path
                    .rewritePath("/api/v1/cart/clear", "/internal/users/{{USER_ID}}/cart/clear"))
                .uri("http://localhost:8081"))
                
            // =============================================================================
            // Vehicle Routes (No user context needed - public data)
            // =============================================================================
            .route("vehicles-series-v1", r -> r
                .path("/api/v1/vehicles/series")
                .filters(f -> f
                    .rewritePath("/api/v1/vehicles/series", "/internal/series"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-series-generations-v1", r -> r
                .path("/api/v1/vehicles/series/{seriesCode}/generations")
                .filters(f -> f
                    .rewritePath("/api/v1/vehicles/series/(?<seriesCode>.*)/generations", "/internal/series/${seriesCode}/generations"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-generation-v1", r -> r
                .path("/api/v1/vehicles/generations/{generationCode}")
                .filters(f -> f
                    .rewritePath("/api/v1/vehicles/generations/(?<generationCode>.*)", "/internal/generations/${generationCode}"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-generation-products-v1", r -> r
                .path("/api/v1/vehicles/generations/{generationCode}/products")
                .filters(f -> f
                    .rewritePath("/api/v1/vehicles/generations/(?<generationCode>.*)/products", "/internal/compatibility/${generationCode}/products"))
                .uri("http://localhost:8082"))
                
            // =============================================================================
            // Product Routes (No user context needed - public data)
            // =============================================================================
            .route("products-list-v1", r -> r
                .path("/api/v1/products")
                .filters(f -> f
                    .rewritePath("/api/v1/products", "/internal/products"))
                .uri("http://localhost:8083"))
                
            .route("products-by-sku-v1", r -> r
                .path("/api/v1/products/{sku}")
                .filters(f -> f
                    .rewritePath("/api/v1/products/(?<sku>.*)", "/internal/products/${sku}"))
                .uri("http://localhost:8083"))
                
            .route("products-search-v1", r -> r
                .path("/api/v1/products/search")
                .filters(f -> f
                    .rewritePath("/api/v1/products/search", "/internal/products/search"))
                .uri("http://localhost:8083"))
                
            .route("categories-list-v1", r -> r
                .path("/api/v1/categories")
                .filters(f -> f
                    .rewritePath("/api/v1/categories", "/internal/categories"))
                .uri("http://localhost:8083"))
                
            .route("categories-products-v1", r -> r
                .path("/api/v1/categories/{id}/products")
                .filters(f -> f
                    .rewritePath("/api/v1/categories/(?<id>.*)/products", "/internal/categories/${id}/products"))
                .uri("http://localhost:8083"))
                
            // =============================================================================
            // Convenience Routes (Unversioned - Point to Latest Version)
            // =============================================================================
            
            // Authentication Convenience Routes
            .route("auth-register-latest", r -> r
                .path("/api/auth/register")
                .filters(f -> f
                    .rewritePath("/api/auth/register", "/internal/auth/register"))
                .uri("http://localhost:8081"))
                
            .route("auth-login-latest", r -> r
                .path("/api/auth/login")
                .filters(f -> f
                    .rewritePath("/api/auth/login", "/internal/auth/login"))
                .uri("http://localhost:8081"))
                
            .route("auth-refresh-latest", r -> r
                .path("/api/auth/refresh")
                .filters(f -> f
                    .rewritePath("/api/auth/refresh", "/internal/auth/refresh-token"))
                .uri("http://localhost:8081"))
                
            .route("auth-logout-latest", r -> r
                .path("/api/auth/logout")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .rewritePath("/api/auth/logout", "/internal/auth/logout"))
                .uri("http://localhost:8081"))
                
            .route("auth-me-latest", r -> r
                .path("/api/auth/me")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .rewritePath("/api/auth/me", "/internal/auth/validate-token"))
                .uri("http://localhost:8081"))

            // Vehicle Management Convenience Routes
            .route("vehicles-series-latest", r -> r
                .path("/api/vehicles/series")
                .filters(f -> f
                    .rewritePath("/api/vehicles/series", "/internal/series"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-series-generations-latest", r -> r
                .path("/api/vehicles/series/{seriesCode}/generations")
                .filters(f -> f
                    .rewritePath("/api/vehicles/series/(?<seriesCode>.*)/generations", "/internal/series/${seriesCode}/generations"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-generation-latest", r -> r
                .path("/api/vehicles/generations/{generationCode}")
                .filters(f -> f
                    .rewritePath("/api/vehicles/generations/(?<generationCode>.*)", "/internal/generations/${generationCode}"))
                .uri("http://localhost:8082"))
                
            .route("vehicles-generation-products-latest", r -> r
                .path("/api/vehicles/generations/{generationCode}/products")
                .filters(f -> f
                    .rewritePath("/api/vehicles/generations/(?<generationCode>.*)/products", "/internal/compatibility/${generationCode}/products"))
                .uri("http://localhost:8082"))

            // Product Catalog Convenience Routes
            .route("products-list-latest", r -> r
                .path("/api/products")
                .filters(f -> f
                    .rewritePath("/api/products", "/internal/products"))
                .uri("http://localhost:8083"))
                
            .route("products-by-sku-latest", r -> r
                .path("/api/products/{sku}")
                .filters(f -> f
                    .rewritePath("/api/products/(?<sku>.*)", "/internal/products/${sku}"))
                .uri("http://localhost:8083"))
                
            .route("products-search-latest", r -> r
                .path("/api/products/search")
                .filters(f -> f
                    .rewritePath("/api/products/search", "/internal/products/search"))
                .uri("http://localhost:8083"))
                
            .route("categories-list-latest", r -> r
                .path("/api/categories")
                .filters(f -> f
                    .rewritePath("/api/categories", "/internal/categories"))
                .uri("http://localhost:8083"))
                
            .route("categories-products-latest", r -> r
                .path("/api/categories/{id}/products")
                .filters(f -> f
                    .rewritePath("/api/categories/(?<id>.*)/products", "/internal/categories/${id}/products"))
                .uri("http://localhost:8083"))

            // Shopping Cart Convenience Routes (with user context)
            .route("cart-get-latest", r -> r
                .path("/api/cart")
                .and().method("GET")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .filter(new UserContextFilter())
                    .rewritePath("/api/cart", "/internal/users/{{USER_ID}}/cart"))
                .uri("http://localhost:8081"))
                
            .route("cart-add-item-latest", r -> r
                .path("/api/cart/items")
                .and().method("POST")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .filter(new UserContextFilter())
                    .rewritePath("/api/cart/items", "/internal/users/{{USER_ID}}/cart/items"))
                .uri("http://localhost:8081"))
                
            .route("cart-update-item-latest", r -> r
                .path("/api/cart/items/{itemId}")
                .and().method("PUT")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .filter(new UserContextFilter())
                    .rewritePath("/api/cart/items/(?<itemId>.*)", "/internal/users/{{USER_ID}}/cart/items/${itemId}"))
                .uri("http://localhost:8081"))
                
            .route("cart-delete-item-latest", r -> r
                .path("/api/cart/items/{itemId}")
                .and().method("DELETE")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .filter(new UserContextFilter())
                    .rewritePath("/api/cart/items/(?<itemId>.*)", "/internal/users/{{USER_ID}}/cart/items/${itemId}"))
                .uri("http://localhost:8081"))
                
            .route("cart-clear-latest", r -> r
                .path("/api/cart/clear")
                .and().method("DELETE")
                .filters(f -> f
                    .filter(new JwtAuthenticationFilter())
                    .filter(new UserContextFilter())
                    .rewritePath("/api/cart/clear", "/internal/users/{{USER_ID}}/cart/clear"))
                .uri("http://localhost:8081"))

            // =============================================================================
            // Admin Routes (Versioned)
            // =============================================================================
            
            .route("admin-products-create-v1", r -> r
                .path("/api/v1/admin/products")
                .and().method("POST")
                .filters(f -> f
                    .rewritePath("/api/v1/admin/products", "/internal/products"))
                .uri("http://localhost:8083"))
                
            .route("admin-products-update-v1", r -> r
                .path("/api/v1/admin/products/{sku}")
                .and().method("PUT")
                .filters(f -> f
                    .rewritePath("/api/v1/admin/products/(?<sku>.*)", "/internal/products/${sku}"))
                .uri("http://localhost:8083"))
                
            .route("admin-vehicles-create-v1", r -> r
                .path("/api/v1/admin/vehicles")
                .and().method("POST")
                .filters(f -> f
                    .rewritePath("/api/v1/admin/vehicles", "/internal/series"))
                .uri("http://localhost:8082"))

            // =============================================================================
            // Health & Monitoring Routes (for development/testing)
            // =============================================================================
            
            .route("health-user", r -> r
                .path("/health/user/**")
                .filters(f -> f
                    .rewritePath("/health/user/(?<segment>.*)", "/${segment}"))
                .uri("http://localhost:8081"))
                
            .route("health-vehicle", r -> r
                .path("/health/vehicle/**")
                .filters(f -> f
                    .rewritePath("/health/vehicle/(?<segment>.*)", "/${segment}"))
                .uri("http://localhost:8082"))
                
            .route("health-product", r -> r
                .path("/health/product/**")
                .filters(f -> f
                    .rewritePath("/health/product/(?<segment>.*)", "/${segment}"))
                .uri("http://localhost:8083"))
                
            .build();
    }
}
