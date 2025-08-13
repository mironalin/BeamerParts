package live.alinmiron.beamerparts.product.controller.admin;

/**
 * OpenAPI specifications for AdminCategoryController endpoints
 * Keeps controller clean by centralizing all Swagger documentation for admin category management
 */
public final class AdminCategoryControllerOpenApiSpec {
    
    private AdminCategoryControllerOpenApiSpec() {
        // Utility class - prevent instantiation
    }
    
    // ========================================
    // POST /admin/categories - Create Category
    // ========================================
    
    public static final String CREATE_CATEGORY_SUMMARY = "Create a new category";
    
    public static final String CREATE_CATEGORY_DESCRIPTION = """
        Create a new product category in the BMW parts catalog with hierarchical support.
        
        **Administrative Operation:**
        - Requires admin authentication and appropriate permissions
        - Creates category with hierarchical support (can have parent category)
        - Validates slug uniqueness across the system
        - Automatically sets creation timestamp
        
        **Category Hierarchy:**
        - Root categories: parentId = null (e.g., "Engine Parts", "Brake System")
        - Subcategories: parentId set to parent category ID
        - Supports multiple levels of nesting
        - Display order controls sorting within each level
        
        **Required Fields:**
        - Name: Category display name (max 100 characters)
        - Slug: URL-friendly identifier (must be unique, max 100 characters)
        - Display Order: Controls sorting order (0-based)
        
        **Optional Fields:**
        - Description: Detailed category information (max 1000 characters)
        - Parent ID: For creating subcategories
        - Is Active: Enable/disable category (default: true)
        
        **Validation Rules:**
        - Slug must be unique across all categories
        - Parent category must exist and be active
        - Display order must be non-negative
        - Name and slug cannot be blank
        
        **Use Cases:**
        - Creating main categories: "Interior", "Exterior", "Engine"
        - Creating subcategories: "Interior > Seats", "Engine > Filters"
        - Organizing product catalog hierarchy
        - SEO-friendly URL structure with slugs
        
        **Examples:**
        - Root category: {"name": "Brake System", "slug": "brake-system", "displayOrder": 1}
        - Subcategory: {"name": "Brake Pads", "slug": "brake-pads", "parentId": 1, "displayOrder": 0}
        """;
    
    // ========================================
    // PUT /admin/categories/{id} - Update Category
    // ========================================
    
    public static final String UPDATE_CATEGORY_SUMMARY = "Update existing category";
    
    public static final String UPDATE_CATEGORY_DESCRIPTION = """
        Update an existing category's information using its ID.
        
        **Administrative Operation:**
        - Requires admin authentication and appropriate permissions
        - Updates all provided fields while preserving existing values for omitted fields
        - Maintains category hierarchy relationships
        - Updates modification timestamp automatically
        
        **Update Capabilities:**
        - Change category name and description
        - Modify slug (with uniqueness validation)
        - Update display order for reordering
        - Change parent category (move in hierarchy)
        - Enable/disable category status
        
        **Business Rules:**
        - Slug uniqueness is enforced (except for current category)
        - Cannot set category as its own parent (prevents cycles)
        - Moving category updates all child category paths
        - Disabling category affects product visibility
        
        **Impact of Changes:**
        - Name changes affect navigation and SEO
        - Slug changes may break existing URLs (consider redirects)
        - Parent changes reorganize category hierarchy
        - Display order changes affect catalog browsing
        - Status changes affect product discoverability
        
        **Error Handling:**
        - Returns 404 if category with ID doesn't exist
        - Returns 400 for validation errors (duplicate slug, invalid parent)
        - Returns 409 if hierarchy constraints are violated
        """;
    
    public static final String PARAM_CATEGORY_ID_DESCRIPTION = """
        Category ID to update. Must be a valid existing category identifier.
        """;
    
    public static final String PARAM_CATEGORY_ID_EXAMPLE = "1";
    
    // ========================================
    // DELETE /admin/categories/{id} - Delete Category
    // ========================================
    
    public static final String DELETE_CATEGORY_SUMMARY = "Delete category (soft delete)";
    
    public static final String DELETE_CATEGORY_DESCRIPTION = """
        Perform a soft delete on a category by setting its status to inactive.
        
        **Soft Delete Approach:**
        - Category is not physically removed from database
        - Status is changed to inactive to hide from customer-facing APIs
        - Preserves historical data and relationships
        - Maintains referential integrity with products
        
        **Administrative Operation:**
        - Requires admin authentication and delete permissions
        - Cannot be undone through API (requires direct database access)
        - Should be used cautiously as it affects catalog organization
        
        **Impact on System:**
        - Category disappears from customer navigation
        - Products in category may become harder to find
        - Subcategories are also effectively hidden
        - Product relationships are preserved for reporting
        
        **Business Rules:**
        - Cannot delete categories with active products
        - Cannot delete categories with active subcategories
        - Must move or delete child content first
        
        **Considerations Before Deletion:**
        - Check if category has active products
        - Consider moving products to another category first
        - Notify relevant teams about catalog changes
        - Plan for URL redirects if needed
        
        **Alternative Actions:**
        - Move products to appropriate alternative categories
        - Merge with similar existing categories
        - Use update endpoint to disable temporarily
        
        **Recovery:**
        - Soft-deleted categories can be restored via database
        - Contact system administrator for recovery procedures
        - Full audit trail available for compliance
        """;
    
    public static final String PARAM_DELETE_CATEGORY_ID_DESCRIPTION = """
        Category ID to delete. The category must not have active products or subcategories.
        """;
    
    public static final String PARAM_DELETE_CATEGORY_ID_EXAMPLE = "1";
    
    // ========================================
    // Request/Response Body Descriptions
    // ========================================
    
    public static final String REQUEST_BODY_CREATE_CATEGORY_DESCRIPTION = """
        Category information for creating a new category entry.
        All required fields must be provided to establish proper hierarchy.
        """;
    
    public static final String REQUEST_BODY_CREATE_CATEGORY_EXAMPLE = """
        {
          "name": "Brake System",
          "slug": "brake-system",
          "description": "Complete brake system components including pads, discs, calipers, and brake fluid for BMW vehicles. Essential safety components requiring regular maintenance and quality parts.",
          "parentId": null,
          "displayOrder": 1,
          "isActive": true
        }
        """;
    
    public static final String REQUEST_BODY_UPDATE_CATEGORY_DESCRIPTION = """
        Updated category information. Only provided fields will be changed,
        omitted fields will retain their current values. Validation applies to all fields.
        """;
    
    public static final String REQUEST_BODY_UPDATE_CATEGORY_EXAMPLE = """
        {
          "name": "Complete Brake System",
          "slug": "complete-brake-system",
          "description": "Comprehensive brake system components and accessories for all BMW models. Includes premium quality pads, rotors, calipers, and brake maintenance items.",
          "parentId": null,
          "displayOrder": 1,
          "isActive": true
        }
        """;
    
    public static final String RESPONSE_BODY_CATEGORY_DESCRIPTION = """
        Complete category information including system-generated fields like ID and timestamps.
        Contains all category hierarchy data and metadata for administrative purposes.
        """;
    
    public static final String RESPONSE_BODY_DELETE_SUCCESS_DESCRIPTION = """
        Confirmation of successful deletion. Category is now inactive and hidden from customers.
        The operation cannot be undone through the API.
        """;
    
    // ========================================
    // Common Category Field Descriptions
    // ========================================
    
    public static final String PARAM_CATEGORY_NAME_DESCRIPTION = """
        Category display name shown to customers in navigation and breadcrumbs.
        Must be descriptive and user-friendly. Maximum 100 characters.
        """;
    
    public static final String PARAM_CATEGORY_NAME_EXAMPLE = "Brake System";
    
    public static final String PARAM_CATEGORY_SLUG_DESCRIPTION = """
        URL-friendly identifier used in website URLs and API endpoints.
        Must be unique across all categories. Use lowercase, hyphens for spaces.
        """;
    
    public static final String PARAM_CATEGORY_SLUG_EXAMPLE = "brake-system";
    
    public static final String PARAM_CATEGORY_DESCRIPTION_DESCRIPTION = """
        Detailed description of the category for SEO and customer information.
        Helps customers understand what products belong in this category.
        """;
    
    public static final String PARAM_CATEGORY_DESCRIPTION_EXAMPLE = "Complete brake system components for BMW vehicles";
    
    public static final String PARAM_PARENT_ID_DESCRIPTION = """
        Parent category ID for creating subcategories. Leave null for root categories.
        Parent must exist and be active.
        """;
    
    public static final String PARAM_PARENT_ID_EXAMPLE = "1";
    
    public static final String PARAM_DISPLAY_ORDER_DESCRIPTION = """
        Sort order within the category level. Lower numbers appear first.
        Used to control the order categories appear in navigation.
        """;
    
    public static final String PARAM_DISPLAY_ORDER_EXAMPLE = "1";
    
    public static final String PARAM_IS_ACTIVE_DESCRIPTION = """
        Whether the category is active and visible to customers.
        Inactive categories are hidden from navigation but preserve data.
        """;
    
    public static final String PARAM_IS_ACTIVE_EXAMPLE = "true";
}
