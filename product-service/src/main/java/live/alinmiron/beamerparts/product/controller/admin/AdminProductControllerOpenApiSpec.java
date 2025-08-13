package live.alinmiron.beamerparts.product.controller.admin;

/**
 * OpenAPI specifications for AdminProductController endpoints
 * Keeps controller clean by centralizing all Swagger documentation for admin product management
 */
public final class AdminProductControllerOpenApiSpec {
    
    private AdminProductControllerOpenApiSpec() {
        // Utility class - prevent instantiation
    }
    
    // ========================================
    // POST /admin/products - Create Product
    // ========================================
    
    public static final String CREATE_PRODUCT_SUMMARY = "Create a new product";
    
    public static final String CREATE_PRODUCT_DESCRIPTION = """
        Create a new product in the BMW parts catalog with all required details.
        
        **Administrative Operation:**
        - Requires admin authentication and appropriate permissions
        - Creates product with ACTIVE status by default
        - Generates unique product ID automatically
        - Validates all business rules and constraints
        
        **Required Fields:**
        - SKU: Must be unique across the system (format: BRAND-GENERATION-CATEGORY-SEQUENCE)
        - Name: Product display name for customers
        - Base Price: Product price in the system's base currency
        - Category: Valid category code from the system
        - Brand: Typically 'BMW' for OEM parts
        
        **Optional Fields:**
        - Description: Detailed product information
        - Featured: Whether to highlight this product (default: false)
        - Images: Product image URLs
        - Compatibility: BMW generation codes this product fits
        
        **Validation Rules:**
        - SKU format validation (BRAND-GENERATION-CATEGORY-SEQUENCE)
        - Price must be positive decimal
        - Category must exist in the system
        - Compatibility generations must be valid codes
        
        **Post-Creation:**
        - Product becomes immediately available for customer browsing
        - Inventory record is created automatically with 0 stock
        - Full-text search index is updated for discoverability
        
        **Examples:**
        - Creating brake pads: SKU="BMW-F30-BR-PAD-FRONT", Category="brake-system"
        - Creating engine parts: SKU="BMW-E90-ENG-FILTER-001", Category="engine-parts"
        """;
    
    // ========================================
    // PUT /admin/products/{sku} - Update Product
    // ========================================
    
    public static final String UPDATE_PRODUCT_SUMMARY = "Update existing product";
    
    public static final String UPDATE_PRODUCT_DESCRIPTION = """
        Update an existing product's information using its SKU identifier.
        
        **Administrative Operation:**
        - Requires admin authentication and appropriate permissions
        - Updates all provided fields, maintains existing values for omitted fields
        - Preserves product ID and creation timestamp
        - Updates modification timestamp automatically
        
        **Update Capabilities:**
        - Change product name, description, and pricing
        - Modify category assignment (with validation)
        - Update featured status for marketing purposes
        - Add/remove BMW generation compatibility
        - Update image URLs and display order
        
        **Business Rules:**
        - Cannot change SKU (use this endpoint's path parameter instead)
        - Price changes take effect immediately for new orders
        - Category changes must be to valid existing categories
        - Status can be changed (ACTIVE, INACTIVE, DISCONTINUED)
        
        **Impact of Changes:**
        - Name/description changes update search index immediately
        - Price changes affect new orders but not existing ones
        - Category changes may affect product discoverability
        - Featured status changes affect homepage/marketing displays
        - Compatibility changes affect generation-based search results
        
        **Error Handling:**
        - Returns 404 if product with SKU doesn't exist
        - Returns 400 for validation errors (invalid category, negative price, etc.)
        - Returns 409 if business rules are violated
        
        **Audit Trail:**
        - All changes are logged for compliance and troubleshooting
        - Previous values are preserved in audit history
        - Modification timestamp and admin user are recorded
        """;
    
    public static final String PARAM_PRODUCT_SKU_DESCRIPTION = """
        Product SKU (Stock Keeping Unit) - unique identifier for the product to update.
        Must be exact match, case-sensitive. The product must exist in the system.
        Format: BRAND-GENERATION-CATEGORY-SEQUENCE (e.g., BMW-F30-AC-001)
        """;
    
    public static final String PARAM_PRODUCT_SKU_EXAMPLE = "BMW-F30-AC-001";
    
    // ========================================
    // DELETE /admin/products/{sku} - Delete Product
    // ========================================
    
    public static final String DELETE_PRODUCT_SUMMARY = "Delete product (soft delete)";
    
    public static final String DELETE_PRODUCT_DESCRIPTION = """
        Perform a soft delete on a product by setting its status to INACTIVE.
        
        **Soft Delete Approach:**
        - Product is not physically removed from database
        - Status is changed to INACTIVE to hide from customer-facing APIs
        - Preserves historical data for reporting and audit purposes
        - Maintains referential integrity with orders and inventory
        
        **Administrative Operation:**
        - Requires admin authentication and delete permissions
        - Cannot be undone through API (requires direct database access)
        - Should be used cautiously as it affects customer experience
        
        **Impact on System:**
        - Product immediately disappears from customer browsing
        - Existing orders and order history remain intact
        - Inventory records are preserved but marked as inactive
        - Search index is updated to exclude the product
        - Analytics and reporting data remains available
        
        **When to Use:**
        - Discontinuing a product permanently
        - Removing products with quality issues
        - Cleaning up test/sample products
        - Regulatory compliance requirements
        
        **Alternative to Consider:**
        - Use UPDATE with status=DISCONTINUED for temporary removal
        - This allows easier reactivation if needed
        
        **Business Considerations:**
        - Check for active orders before deletion
        - Notify relevant teams (sales, support, marketing)
        - Consider redirecting product pages to alternatives
        - Update marketing materials and catalogs
        
        **Recovery:**
        - Soft-deleted products can be restored via direct database access
        - Contact system administrator for recovery procedures
        - Full audit trail is available for compliance
        """;
    
    // ========================================
    // Request/Response Body Descriptions
    // ========================================
    
    public static final String REQUEST_BODY_CREATE_PRODUCT_DESCRIPTION = """
        Complete product information for creating a new BMW parts catalog entry.
        All required fields must be provided, optional fields enhance the product listing.
        """;
    
    public static final String REQUEST_BODY_CREATE_PRODUCT_EXAMPLE = """
        {
          "name": "BMW F30 Brake Pad Set - Front",
          "slug": "bmw-f30-brake-pad-set-front",
          "sku": "BMW-F30-BR-PAD-FRONT",
          "description": "High-performance ceramic brake pads designed specifically for BMW F30 3-Series vehicles. Provides excellent stopping power and reduced brake dust. Compatible with standard and M-Sport brake systems.",
          "shortDescription": "Ceramic brake pads for BMW F30 front axle",
          "basePrice": 89.99,
          "categoryId": 1,
          "brand": "BMW",
          "weightGrams": 2500,
          "dimensionsJson": "{\\"length\\": 15.2, \\"width\\": 6.8, \\"height\\": 1.8, \\"unit\\": \\"cm\\"}",
          "isFeatured": false,
          "status": "ACTIVE"
        }
        """;

    public static final String REQUEST_BODY_UPDATE_PRODUCT_DESCRIPTION = """
        Product information to update. Only provided fields will be changed,
        omitted fields will retain their current values. Validation applies to all fields.
        """;

    public static final String REQUEST_BODY_UPDATE_PRODUCT_EXAMPLE = """
        {
          "name": "BMW F30 Brake Pad Set - Front (Updated)",
          "slug": "bmw-f30-brake-pad-set-front-updated",
          "sku": "BMW-F30-BR-PAD-FRONT",
          "description": "Premium ceramic brake pads with enhanced performance characteristics for BMW F30 3-Series. Features improved heat dissipation and longer service life.",
          "shortDescription": "Premium ceramic brake pads for BMW F30",
          "basePrice": 99.99,
          "categoryId": 1,
          "brand": "BMW",
          "weightGrams": 2500,
          "dimensionsJson": "{\\"length\\": 15.2, \\"width\\": 6.8, \\"height\\": 1.8, \\"unit\\": \\"cm\\"}",
          "isFeatured": true,
          "status": "ACTIVE"
        }
        """;    public static final String RESPONSE_BODY_PRODUCT_DESCRIPTION = """
        Complete product information including system-generated fields like ID and timestamps.
        Contains all customer-visible data plus administrative metadata.
        """;
    
    public static final String RESPONSE_BODY_DELETE_SUCCESS_DESCRIPTION = """
        Confirmation of successful deletion. Product is now inactive and hidden from customers.
        The operation cannot be undone through the API.
        """;
}
