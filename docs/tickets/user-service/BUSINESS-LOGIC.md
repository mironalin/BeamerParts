# User Service Business Logic Documentation

## 🎯 **Service Purpose**
User Service handles authentication, authorization, user profile management, and shopping cart functionality. It provides secure user management with JWT-based authentication and integrates with other services for cart operations and user vehicle preferences.

## 🏗️ **Core Domain Concepts**

### **User Lifecycle**
```
Registration → Email Verification → Active User → Profile Management → Cart Usage
     ↓              ↓                   ↓              ↓                ↓
  INACTIVE →    PENDING_VERIFICATION → ACTIVE →   PROFILE_COMPLETE → SHOPPING
```

### **Key Business Entities**

#### **User**
- Central entity for authentication and profile management
- Contains personal information, role assignment, and account status
- Manages relationships with refresh tokens, vehicles, and cart items
- Provides business methods for role validation and account management

#### **UserRefreshToken**
- JWT refresh token management for secure authentication
- Token rotation and expiration handling
- Device/session tracking for security
- Automatic cleanup of expired tokens

#### **UserVehicle**
- User's BMW vehicle preferences and garage
- BMW compatibility integration with vehicle-service
- Primary vehicle selection for parts filtering
- Vehicle-specific part recommendations

#### **CartItem**
- Shopping cart item management
- Real-time inventory validation with product-service
- Price preservation and session management
- BMW compatibility validation for user's vehicles

## 🧠 **Critical Business Logic**

### **Authentication & Security**
**Purpose**: Secure user authentication with JWT tokens and role-based authorization.

**Password Security**:
```java
public class AuthenticationDomainService {
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final int MAX_LOGIN_ATTEMPTS = 5;
    private final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);
    
    public User registerUser(UserRegistrationRequest request) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        
        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // Create user with default role
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(hashedPassword)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(UserRole.CUSTOMER)
            .isActive(true)
            .emailVerified(false)
            .loginAttempts(0)
            .build();
        
        User savedUser = userRepository.save(user);
        
        // Send email verification
        emailService.sendVerificationEmail(savedUser);
        
        // Publish user registered event
        eventPublisher.publishUserRegistered(UserRegisteredEvent.from(savedUser));
        
        return savedUser;
    }
    
    public AuthTokens authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        // Check account lockout
        if (user.isAccountLocked()) {
            if (user.getLockoutExpiresAt().isAfter(LocalDateTime.now())) {
                throw new AccountLockedException("Account is locked until " + user.getLockoutExpiresAt());
            } else {
                // Unlock account if lockout period has expired
                user.unlockAccount();
            }
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Increment failed login attempts
            user.incrementLoginAttempts();
            
            if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.lockAccount(LOCKOUT_DURATION);
                throw new AccountLockedException("Account locked due to too many failed login attempts");
            }
            
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        // Reset login attempts on successful authentication
        user.resetLoginAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate JWT tokens
        return jwtTokenService.generateTokens(user);
    }
}
```

**JWT Token Management**:
```java
public class JwtTokenService {
    
    private final String jwtSecret;
    private final Duration accessTokenExpiry = Duration.ofMinutes(15);
    private final Duration refreshTokenExpiry = Duration.ofDays(7);
    
    public AuthTokens generateTokens(User user) {
        // Generate access token
        String accessToken = generateAccessToken(user);
        
        // Generate refresh token
        String refreshToken = generateRefreshToken();
        
        // Store refresh token in database
        UserRefreshToken refreshTokenEntity = UserRefreshToken.builder()
            .user(user)
            .token(refreshToken)
            .expiresAt(LocalDateTime.now().plus(refreshTokenExpiry))
            .isRevoked(false)
            .build();
        
        refreshTokenRepository.save(refreshTokenEntity);
        
        return AuthTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .accessTokenExpiresAt(LocalDateTime.now().plus(accessTokenExpiry))
            .refreshTokenExpiresAt(LocalDateTime.now().plus(refreshTokenExpiry))
            .build();
    }
    
    private String generateAccessToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("name", user.getFullName())
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(accessTokenExpiry)))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    public TokenValidationResult validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
            
            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            
            return TokenValidationResult.valid(userId, email, role);
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.expired();
        } catch (Exception e) {
            return TokenValidationResult.invalid();
        }
    }
}
```

### **Role-Based Authorization**
**Purpose**: Enforce access control based on user roles and resource ownership.

**Authorization Logic**:
```java
public class AuthorizationDomainService {
    
    public boolean hasPermission(User user, String resource, String action) {
        // Super admin has all permissions
        if (user.isSuperAdmin()) {
            return true;
        }
        
        // Admin permissions
        if (user.isAdmin()) {
            return hasAdminPermission(resource, action);
        }
        
        // Customer permissions
        if (user.isCustomer()) {
            return hasCustomerPermission(resource, action);
        }
        
        return false;
    }
    
    public boolean canAccessResource(User user, String resourceType, Long resourceId) {
        switch (resourceType) {
            case "order":
                return canAccessOrder(user, resourceId);
            case "cart":
                return canAccessCart(user, resourceId);
            case "user_profile":
                return canAccessUserProfile(user, resourceId);
            default:
                return false;
        }
    }
    
    private boolean canAccessOrder(User user, Long orderId) {
        // Users can only access their own orders
        if (user.isCustomer()) {
            Order order = orderRepository.findById(orderId).orElse(null);
            return order != null && order.belongsToUser(user.getId());
        }
        
        // Admins can access all orders
        return user.isAdmin();
    }
    
    private boolean hasAdminPermission(String resource, String action) {
        // Define admin permissions
        Map<String, Set<String>> adminPermissions = Map.of(
            "users", Set.of("read", "update", "delete"),
            "orders", Set.of("read", "update", "cancel"),
            "products", Set.of("read", "create", "update", "delete"),
            "analytics", Set.of("read")
        );
        
        return adminPermissions.getOrDefault(resource, Set.of()).contains(action);
    }
    
    private boolean hasCustomerPermission(String resource, String action) {
        // Define customer permissions
        Map<String, Set<String>> customerPermissions = Map.of(
            "profile", Set.of("read", "update"),
            "cart", Set.of("read", "create", "update", "delete"),
            "orders", Set.of("read", "create"),
            "vehicles", Set.of("read", "create", "update", "delete")
        );
        
        return customerPermissions.getOrDefault(resource, Set.of()).contains(action);
    }
}
```

### **Shopping Cart Management**
**Purpose**: Manage user shopping cart with real-time inventory validation and BMW compatibility.

**Cart Business Logic**:
```java
public class CartDomainService {
    
    public CartItem addItemToCart(Long userId, String productSku, Integer quantity) {
        // Validate user exists and is active
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        if (!user.isActive()) {
            throw new InactiveUserException("Cannot add items to cart for inactive user");
        }
        
        // Validate product exists and is available
        ProductInternalDto product = productServiceClient.getProduct(productSku);
        if (!product.isActive()) {
            throw new ProductNotAvailableException("Product " + productSku + " is not available");
        }
        
        // Check inventory availability
        InventoryCheckResult inventoryCheck = productServiceClient.checkInventory(productSku, quantity);
        if (!inventoryCheck.isAvailable()) {
            throw new InsufficientStockException(
                String.format("Only %d units available for product %s", 
                    inventoryCheck.getAvailableQuantity(), productSku));
        }
        
        // Check BMW compatibility with user's vehicles
        if (user.hasVehicles()) {
            boolean isCompatible = validateBmwCompatibility(user, productSku);
            if (!isCompatible) {
                // Add warning but allow addition (user choice)
                log.warn("Product {} may not be compatible with user {}'s vehicles", productSku, userId);
            }
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductSku(userId, productSku);
        
        if (existingItem.isPresent()) {
            // Update existing item quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            
            // Validate total quantity against inventory
            InventoryCheckResult totalCheck = productServiceClient.checkInventory(productSku, newQuantity);
            if (!totalCheck.isAvailable()) {
                throw new InsufficientStockException(
                    String.format("Cannot add %d more units. Only %d total units available", 
                        quantity, totalCheck.getAvailableQuantity()));
            }
            
            item.setQuantity(newQuantity);
            item.setPrice(product.getPrice()); // Update to current price
            item.setUpdatedAt(LocalDateTime.now());
            
            return cartItemRepository.save(item);
        } else {
            // Create new cart item
            CartItem newItem = CartItem.builder()
                .user(user)
                .productSku(productSku)
                .quantity(quantity)
                .price(product.getPrice())
                .addedAt(LocalDateTime.now())
                .build();
            
            CartItem savedItem = cartItemRepository.save(newItem);
            
            // Publish cart item added event
            eventPublisher.publishCartItemAdded(CartItemAddedEvent.from(savedItem));
            
            return savedItem;
        }
    }
    
    public CartSummary getCartSummary(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        
        if (cartItems.isEmpty()) {
            return CartSummary.empty();
        }
        
        // Validate all items are still available and get current prices
        List<CartItemSummary> validatedItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (CartItem item : cartItems) {
            try {
                ProductInternalDto product = productServiceClient.getProduct(item.getProductSku());
                InventoryCheckResult inventoryCheck = productServiceClient.checkInventory(
                    item.getProductSku(), item.getQuantity());
                
                CartItemSummary itemSummary = CartItemSummary.builder()
                    .cartItemId(item.getId())
                    .productSku(item.getProductSku())
                    .productName(product.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .currentPrice(product.getPrice())
                    .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .isAvailable(inventoryCheck.isAvailable())
                    .availableQuantity(inventoryCheck.getAvailableQuantity())
                    .priceChanged(!item.getPrice().equals(product.getPrice()))
                    .build();
                
                validatedItems.add(itemSummary);
                
                if (inventoryCheck.isAvailable()) {
                    subtotal = subtotal.add(itemSummary.getTotalPrice());
                }
            } catch (ProductNotFoundException e) {
                // Product no longer exists - mark for removal
                cartItemRepository.delete(item);
            }
        }
        
        return CartSummary.builder()
            .items(validatedItems)
            .itemCount(validatedItems.size())
            .subtotal(subtotal)
            .hasUnavailableItems(validatedItems.stream().anyMatch(item -> !item.isAvailable()))
            .hasPriceChanges(validatedItems.stream().anyMatch(CartItemSummary::isPriceChanged))
            .build();
    }
    
    private boolean validateBmwCompatibility(User user, String productSku) {
        List<UserVehicle> userVehicles = userVehicleRepository.findByUserId(user.getId());
        
        for (UserVehicle vehicle : userVehicles) {
            CompatibilityResult result = vehicleServiceClient.validateCompatibility(
                vehicle.getGenerationCode(), productSku);
            
            if (result.isCompatible()) {
                return true; // Compatible with at least one vehicle
            }
        }
        
        return false; // Not compatible with any user vehicles
    }
}
```

### **User Vehicle Management**
**Purpose**: Manage user's BMW vehicle preferences with compatibility validation.

**Vehicle Management Logic**:
```java
public class UserVehicleDomainService {
    
    public UserVehicle addVehicleToUser(Long userId, AddVehicleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Validate BMW generation exists
        BmwGenerationDto generation = vehicleServiceClient.getGeneration(request.getGenerationCode());
        if (generation == null) {
            throw new InvalidBmwGenerationException(request.getGenerationCode());
        }
        
        // Validate year compatibility
        if (request.getYear() != null && !generation.includesYear(request.getYear())) {
            throw new IncompatibleYearException(
                String.format("Year %d is not valid for generation %s (%s)", 
                    request.getYear(), request.getGenerationCode(), generation.getYearRange()));
        }
        
        // Check if user already has this vehicle
        boolean alreadyExists = userVehicleRepository.existsByUserIdAndGenerationCodeAndYear(
            userId, request.getGenerationCode(), request.getYear());
        
        if (alreadyExists) {
            throw new DuplicateVehicleException("User already has this vehicle in their garage");
        }
        
        // Handle primary vehicle selection
        if (request.isPrimary() || !user.hasPrimaryVehicle()) {
            // Remove primary flag from other vehicles
            userVehicleRepository.clearPrimaryVehicleForUser(userId);
        }
        
        // Create user vehicle
        UserVehicle vehicle = UserVehicle.builder()
            .user(user)
            .seriesCode(generation.getSeriesCode())
            .generationCode(request.getGenerationCode())
            .year(request.getYear())
            .modelVariant(request.getModelVariant())
            .isPrimary(request.isPrimary() || !user.hasPrimaryVehicle())
            .build();
        
        UserVehicle savedVehicle = userVehicleRepository.save(vehicle);
        
        // Publish user vehicle added event
        eventPublisher.publishUserVehicleAdded(UserVehicleAddedEvent.from(savedVehicle));
        
        return savedVehicle;
    }
    
    public List<UserVehicle> getUserVehicles(Long userId) {
        return userVehicleRepository.findByUserIdOrderByIsPrimaryDescCreatedAtDesc(userId);
    }
    
    public UserVehicle setPrimaryVehicle(Long userId, Long vehicleId) {
        UserVehicle vehicle = userVehicleRepository.findByIdAndUserId(vehicleId, userId)
            .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        
        // Remove primary flag from all user vehicles
        userVehicleRepository.clearPrimaryVehicleForUser(userId);
        
        // Set this vehicle as primary
        vehicle.setIsPrimary(true);
        
        return userVehicleRepository.save(vehicle);
    }
    
    public List<ProductRecommendationDto> getCompatibleProducts(Long userId, String category) {
        List<UserVehicle> userVehicles = getUserVehicles(userId);
        
        if (userVehicles.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get recommendations for primary vehicle or first vehicle
        UserVehicle primaryVehicle = userVehicles.stream()
            .filter(UserVehicle::getIsPrimary)
            .findFirst()
            .orElse(userVehicles.get(0));
        
        return productServiceClient.getCompatibleProducts(
            primaryVehicle.getGenerationCode(), category);
    }
}
```

## 🔄 **Event-Driven Architecture**

### **Events Published**
```java
// User lifecycle events
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;
    private LocalDateTime registeredAt;
    private String referralSource;
}

public class CartItemAddedEvent {
    private Long userId;
    private String productSku;
    private Integer quantity;
    private BigDecimal price;
    private LocalDateTime addedAt;
}

public class UserVehicleAddedEvent {
    private Long userId;
    private String seriesCode;
    private String generationCode;
    private Integer year;
    private boolean isPrimary;
    private LocalDateTime addedAt;
}
```

### **Events Consumed**
```java
// From product-service
@EventListener
public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
    // Update cart item prices for affected products
    List<CartItem> affectedItems = cartItemRepository.findByProductSku(event.getProductSku());
    
    for (CartItem item : affectedItems) {
        BigDecimal oldPrice = item.getPrice();
        item.setPrice(event.getNewPrice());
        cartItemRepository.save(item);
        
        // Notify user of price change
        emailService.sendPriceChangeNotification(item.getUser(), item, oldPrice, event.getNewPrice());
    }
}

@EventListener
public void handleProductDiscontinued(ProductDiscontinuedEvent event) {
    // Remove discontinued products from all carts
    List<CartItem> itemsToRemove = cartItemRepository.findByProductSku(event.getProductSku());
    
    for (CartItem item : itemsToRemove) {
        cartItemRepository.delete(item);
        
        // Notify user of product removal
        emailService.sendProductDiscontinuedNotification(item.getUser(), item);
    }
}
```

## 🛡️ **Security & Privacy**

### **Data Protection**
```java
public class UserDataProtectionService {
    
    public void anonymizeUserData(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Anonymize personal information (GDPR compliance)
        user.setEmail("deleted-user-" + userId + "@example.com");
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPhone(null);
        user.setIsActive(false);
        
        // Remove refresh tokens
        refreshTokenRepository.deleteByUserId(userId);
        
        // Clear cart
        cartItemRepository.deleteByUserId(userId);
        
        // Remove vehicles
        userVehicleRepository.deleteByUserId(userId);
        
        userRepository.save(user);
    }
    
    public UserDataExport exportUserData(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Collect all user data for export
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        List<UserVehicle> vehicles = userVehicleRepository.findByUserId(userId);
        
        return UserDataExport.builder()
            .personalInfo(UserPersonalInfo.from(user))
            .cartItems(cartItems.stream().map(CartItemExport::from).collect(toList()))
            .vehicles(vehicles.stream().map(VehicleExport::from).collect(toList()))
            .exportedAt(LocalDateTime.now())
            .build();
    }
}
```

### **Session Management**
```java
public class SessionManagementService {
    
    public void cleanupExpiredSessions() {
        // Remove expired refresh tokens
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        
        // Clear abandoned carts (older than 30 days)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        cartItemRepository.deleteAbandonedCartItems(cutoffDate);
    }
    
    public void revokeAllUserSessions(Long userId) {
        // Revoke all refresh tokens for user
        refreshTokenRepository.revokeAllTokensForUser(userId);
        
        // Clear user cache entries
        cacheManager.getCache("user-sessions").evict(userId);
    }
}
```

This comprehensive business logic documentation provides AI agents with deep understanding of user management, authentication, authorization, cart operations, and BMW vehicle integration needed for successful implementation.
