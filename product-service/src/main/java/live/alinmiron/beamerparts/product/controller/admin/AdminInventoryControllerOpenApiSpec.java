package live.alinmiron.beamerparts.product.controller.admin;

/**
 * OpenAPI specifications for AdminInventoryController endpoints
 * Keeps controller clean by centralizing all Swagger documentation for admin inventory management
 */
public final class AdminInventoryControllerOpenApiSpec {
    
    private AdminInventoryControllerOpenApiSpec() {
        // Utility class - prevent instantiation
    }
    
    // ========================================
    // PUT /admin/inventory/{sku}/stock - Update Stock
    // ========================================
    
    public static final String UPDATE_STOCK_SUMMARY = "Update product stock levels";
    
    public static final String UPDATE_STOCK_DESCRIPTION = """
        Update the inventory stock levels for a specific product identified by SKU.
        
        **Administrative Operation:**
        - Requires admin authentication and inventory management permissions
        - Updates stock quantities across all warehouses/locations
        - Creates audit trail for all stock movements
        - Triggers low-stock alerts if thresholds are crossed
        
        **Stock Management Features:**
        - Set absolute stock levels (replaces current quantity)
        - Add/subtract from current stock (relative adjustments)
        - Update reserved quantities for pending orders
        - Set reorder points and maximum stock levels
        - Configure low-stock alert thresholds
        
        **Business Rules:**
        - Cannot set negative available stock
        - Reserved stock cannot exceed total stock
        - System validates against pending order requirements
        - Automatic stock allocation for confirmed orders
        
        **Warehouse Integration:**
        - Updates reflect across all connected warehouse systems
        - Multi-location inventory supported
        - Real-time stock synchronization
        - Handles stock transfers between locations
        
        **Notifications & Alerts:**
        - Low stock alerts sent to procurement team
        - High stock alerts for overstock situations
        - Stock discrepancy notifications for auditing
        - Integration with purchasing system for reorders
        
        **Use Cases:**
        - Receiving new inventory shipments
        - Adjusting for damaged/returned items
        - Correcting inventory discrepancies
        - Setting initial stock for new products
        - Managing seasonal stock adjustments
        
        **Audit & Compliance:**
        - All changes logged with timestamp and admin user
        - Supports inventory auditing requirements
        - Integration with accounting systems
        - Compliance with financial reporting standards
        
        **Performance Considerations:**
        - Stock updates are processed in real-time
        - Customer-facing availability updated immediately
        - Background processes handle complex calculations
        - Optimistic locking prevents concurrent update conflicts
        """;
    
    public static final String PARAM_INVENTORY_SKU_DESCRIPTION = """
        Product SKU (Stock Keeping Unit) for inventory management.
        Must match exactly with an existing product in the system.
        Format: BRAND-GENERATION-CATEGORY-SEQUENCE (e.g., BMW-F30-AC-001)
        """;
    
    public static final String PARAM_INVENTORY_SKU_EXAMPLE = "BMW-F30-AC-001";
    
    // ========================================
    // GET /admin/inventory/{sku} - Get Inventory Details
    // ========================================
    
    public static final String GET_INVENTORY_SUMMARY = "Get detailed inventory information";
    
    public static final String GET_INVENTORY_DESCRIPTION = """
        Retrieve comprehensive inventory details for a specific product by SKU.
        
        **Administrative View:**
        - Provides complete inventory status and history
        - Includes all stock locations and movements
        - Shows pending orders and reserved quantities
        - Displays audit trail and change history
        
        **Inventory Information Included:**
        - Total available stock across all locations
        - Reserved stock for pending/confirmed orders
        - Stock levels by warehouse location
        - Reorder points and maximum stock levels
        - Last restock date and next expected delivery
        
        **Stock Movement History:**
        - Recent stock adjustments and reasons
        - Receiving records and supplier information
        - Sales and order fulfillment data
        - Transfer records between locations
        - Audit adjustments and corrections
        
        **Financial Information:**
        - Current stock value at cost price
        - Average cost per unit calculation
        - Stock turnover rates and trends
        - Carrying cost analysis
        - Write-off and shrinkage tracking
        
        **Operational Metrics:**
        - Days of inventory remaining
        - Stock velocity and movement patterns
        - Seasonal demand variations
        - Supplier lead times and reliability
        - Customer demand forecasting data
        
        **Alert & Warning Indicators:**
        - Low stock warnings and critical levels
        - Overstock situations requiring action
        - Slow-moving inventory alerts
        - Expired or aging stock notifications
        - Supplier delivery delays
        
        **Integration Data:**
        - ERP system synchronization status
        - Warehouse management system data
        - Point-of-sale system integration
        - E-commerce platform stock sync
        - Third-party logistics provider info
        
        **Decision Support:**
        - Recommended reorder quantities
        - Optimal stock level suggestions
        - Seasonal adjustment recommendations
        - Supplier performance metrics
        - Cost optimization opportunities
        
        **Use Cases:**
        - Daily inventory management review
        - Stock planning and procurement decisions
        - Financial reporting and analysis
        - Compliance auditing and verification
        - Performance monitoring and optimization
        """;
    
    // ========================================
    // Request/Response Body Descriptions
    // ========================================
    
    public static final String REQUEST_BODY_UPDATE_STOCK_DESCRIPTION = """
        Stock update information including new quantities, adjustment reasons, and location details.
        Supports both absolute quantity setting and relative adjustments (add/subtract).
        """;
    
    public static final String RESPONSE_BODY_INVENTORY_DESCRIPTION = """
        Complete inventory information including current stock levels, locations, history,
        and administrative metadata. Provides full visibility for inventory management decisions.
        """;
    
    public static final String RESPONSE_BODY_UPDATE_STOCK_SUCCESS_DESCRIPTION = """
        Updated inventory information reflecting the new stock levels.
        Includes confirmation of changes and current stock status across all locations.
        """;
    
    // ========================================
    // Common Parameter Descriptions
    // ========================================
    
    public static final String PARAM_STOCK_QUANTITY_DESCRIPTION = """
        New stock quantity to set. Must be a non-negative integer.
        Represents total available units across all locations.
        """;
    
    public static final String PARAM_STOCK_QUANTITY_EXAMPLE = "150";
    
    public static final String PARAM_ADJUSTMENT_REASON_DESCRIPTION = """
        Reason for the stock adjustment. Used for audit trail and reporting.
        Common values: 'RECEIVED_SHIPMENT', 'DAMAGED_GOODS', 'INVENTORY_COUNT', 'CUSTOMER_RETURN'
        """;
    
    public static final String PARAM_ADJUSTMENT_REASON_EXAMPLE = "RECEIVED_SHIPMENT";
    
    public static final String PARAM_WAREHOUSE_LOCATION_DESCRIPTION = """
        Warehouse or location identifier where the stock is located.
        Used for multi-location inventory management.
        """;
    
    public static final String PARAM_WAREHOUSE_LOCATION_EXAMPLE = "WAREHOUSE_MAIN";
}
