package live.alinmiron.beamerparts.product.controller.external;

/**
 * OpenAPI specifications for ProductController endpoints
 * Keeps controller clean by centralizing all Swagger documentation
 */
public final class ProductControllerOpenApiSpec {
    
    private ProductControllerOpenApiSpec() {
        // Utility class - prevent instantiation
    }
    
    // ========================================
    // GET /products - Get All Products
    // ========================================
    
    public static final String GET_ALL_PRODUCTS_SUMMARY = "Get all products with pagination and filtering";
    
    public static final String GET_ALL_PRODUCTS_DESCRIPTION = """
        Retrieve a paginated list of products with optional filtering capabilities.
        
        **Pagination:**
        - Use 'page' (0-based) and 'size' parameters to control pagination
        - Default page size is 20 items
        
        **Sorting:**
        - Use 'sort' parameter with format: property,direction (e.g., 'name,desc')
        - Multiple sort criteria supported: ?sort=name,asc&sort=basePrice,desc
        - Available sort fields: id, name, sku, basePrice, brand, createdAt, updatedAt, status, isFeatured
        
        **Filtering:**
        - Filter by product status (ACTIVE, INACTIVE, DISCONTINUED)
        - Filter by category code
        - Filter to show only featured products
        
        **Examples:**
        - Get first 10 products: ?page=0&size=10
        - Sort by price descending: ?sort=basePrice,desc
        - Filter active brake products: ?category=brake-system&status=ACTIVE
        - Featured products only: ?featured=true
        """;
    
    public static final String PARAM_CATEGORY_CODE_DESCRIPTION = "Filter products by category code (e.g., 'brake-system', 'engine-parts')";
    public static final String PARAM_CATEGORY_CODE_EXAMPLE = "brake-system";
    
    public static final String PARAM_STATUS_DESCRIPTION = "Filter products by status. Available values: ACTIVE (default), INACTIVE, DISCONTINUED";
    public static final String PARAM_STATUS_EXAMPLE = "ACTIVE";
    
    public static final String PARAM_FEATURED_DESCRIPTION = "Filter to show only featured products (true) or include all products (false/null)";
    public static final String PARAM_FEATURED_EXAMPLE = "true";
    
    // ========================================
    // GET /products/{sku} - Get Product by SKU
    // ========================================
    
    public static final String GET_PRODUCT_BY_SKU_SUMMARY = "Get product by SKU";
    
    public static final String GET_PRODUCT_BY_SKU_DESCRIPTION = """
        Retrieve detailed information for a single product using its unique SKU (Stock Keeping Unit).
        
        **About SKUs:**
        - SKU is the unique product identifier used in inventory management
        - Format typically follows pattern: BRAND-GENERATION-CATEGORY-SEQUENCE
        - Case-sensitive exact match required
        
        **Response:**
        - Returns full product details including images, variants, inventory, and compatibility data
        - Includes category information and BMW generation compatibility
        - Shows current stock levels and pricing information
        
        **Error Handling:**
        - Returns 404 if product with specified SKU doesn't exist
        - Returns 404 if product exists but is not ACTIVE status
        
        **Examples:**
        - BMW-F30-AC-001 (BMW F30 Air Conditioning part #001)
        - BMW-E90-BR-PAD-FRONT (BMW E90 Brake Pad Front)
        """;
    
    public static final String PARAM_SKU_DESCRIPTION = """
        Product SKU (Stock Keeping Unit) - unique identifier for the product.
        Must be exact match, case-sensitive. Format: BRAND-GENERATION-CATEGORY-SEQUENCE
        """;
    public static final String PARAM_SKU_EXAMPLE = "BMW-F30-AC-001";
    
    // ========================================
    // GET /products/search - Search Products
    // ========================================
    
    public static final String SEARCH_PRODUCTS_SUMMARY = "Search products by text query";
    
    public static final String SEARCH_PRODUCTS_DESCRIPTION = """
        Perform full-text search across product names and descriptions using PostgreSQL's advanced search capabilities.
        
        **Search Features:**
        - Searches both product name and description fields
        - Case-insensitive matching
        - Supports word stemming (e.g., 'handle' matches 'handles', 'handling')
        - English language processing for better relevance
        - Multiple word searches (e.g., 'brake pad' finds products with both words)
        
        **Search Examples:**
        - 'handle' - finds all products with 'handle' in name or description
        - 'brake pad' - finds products containing both 'brake' AND 'pad'
        - 'BMW door' - finds BMW door-related products
        - 'F30 parts' - finds parts compatible with F30 generation
        
        **Filters:**
        - Combine search with category, price range filters
        - Only returns ACTIVE products
        - Supports pagination and sorting
        
        **Sorting:**
        - Default: relevance-based ordering
        - Custom sorting available using standard sort parameters
        """;
    
    public static final String PARAM_SEARCH_QUERY_DESCRIPTION = """
        Search query text. Searches in product names and descriptions.
        Supports multiple words, stemming, and PostgreSQL full-text search features.
        """;
    public static final String PARAM_SEARCH_QUERY_EXAMPLE = "brake pad";
    
    public static final String PARAM_CATEGORY_FILTER_DESCRIPTION = "Optional: Filter results by category code (e.g., 'brake-system', 'engine-parts')";
    public static final String PARAM_CATEGORY_FILTER_EXAMPLE = "brake-system";
    
    public static final String PARAM_MIN_PRICE_DESCRIPTION = "Optional: Minimum price filter in product's base currency. Use decimal format.";
    public static final String PARAM_MIN_PRICE_EXAMPLE = "25.50";
    
    public static final String PARAM_MAX_PRICE_DESCRIPTION = "Optional: Maximum price filter in product's base currency. Use decimal format.";
    public static final String PARAM_MAX_PRICE_EXAMPLE = "150.00";
    
    // ========================================
    // GET /products/featured - Get Featured Products
    // ========================================
    
    public static final String GET_FEATURED_PRODUCTS_SUMMARY = "Get featured products";
    
    public static final String GET_FEATURED_PRODUCTS_DESCRIPTION = """
        Retrieve a list of products that have been marked as 'featured' by administrators.
        
        **What are Featured Products:**
        - Hand-picked products highlighted for promotion
        - Often include: best sellers, staff picks, new arrivals, or sale items
        - Commonly displayed on homepage, in marketing campaigns, or for cross-selling
        
        **Behavior:**
        - Only returns products with featured=true AND status=ACTIVE
        - Ordered by default database ordering (typically creation time or ID)
        - No pagination - returns simple list with optional limit
        
        **Use Cases:**
        - Homepage product showcase
        - 'Recommended for you' sections
        - Marketing campaign highlights
        - Cross-selling suggestions
        
        **Performance:**
        - Optimized with database indexes on featured and status fields
        - Cached results recommended for high-traffic usage
        """;
    
    public static final String PARAM_LIMIT_DESCRIPTION = """
        Maximum number of featured products to return. 
        Defaults to 10 if not specified. Set to higher value (e.g., 100) to get all featured products.
        """;
    public static final String PARAM_LIMIT_EXAMPLE = "10";
    
    // ========================================
    // GET /products/by-generation/{generationCode} - Get Products by Generation
    // ========================================
    
    public static final String GET_PRODUCTS_BY_GENERATION_SUMMARY = "Get products by BMW generation compatibility";
    
    public static final String GET_PRODUCTS_BY_GENERATION_DESCRIPTION = """
        Find all products that are compatible with a specific BMW generation code.
        
        **BMW Generation Codes:**
        - E-Series: E30, E36, E46, E90, E91, E92, E93 (older generations)
        - F-Series: F10, F20, F30, F31, F32, F33, F34, F36 (newer generations)
        - G-Series: G20, G21, G22, G23, G30, G31, G32 (latest generations)
        
        **Compatibility Detection:**
        - Explicit compatibility records in the database
        - Smart pattern matching on product SKUs (e.g., BMW-F30-BR-PAD-FRONT)
        - Pattern matching on product names (e.g., "BMW F30 Brake Pad Set")
        - Only ACTIVE products are included in results
        
        **Use Cases:**
        - "Show me all parts for my BMW F30"
        - Parts catalog filtered by vehicle generation
        - Compatibility verification for ordering
        - Cross-reference for part replacement
        
        **Category Filtering:**
        - Use the 'category' parameter to narrow results by part type
        - Example: ?category=brake-system to show only brake parts for F30
        - Category codes: brake-system, engine, suspension, etc.
        """;
    
    public static final String PARAM_GENERATION_CODE_DESCRIPTION = """
        BMW generation code (e.g., F30, E90, G20).
        Case-sensitive. Must match exactly with generation codes in the system.
        Common examples: E46, E90, F10, F30, G20, G30
        """;
    public static final String PARAM_GENERATION_CODE_EXAMPLE = "F30";
    
    public static final String PARAM_CATEGORY_BY_GENERATION_DESCRIPTION = """
        Optional: Filter results by category code to narrow down the parts type.
        Example: 'brake-system' to show only brake-related parts for the generation.
        """;
    public static final String PARAM_CATEGORY_BY_GENERATION_EXAMPLE = "brake-system";
}
