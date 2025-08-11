package live.alinmiron.beamerparts.product.entity;

/**
 * Stock movement type enumeration
 * Maps to the movement_type column constraint in stock_movements table
 */
public enum StockMovementType {
    INCOMING,
    OUTGOING,
    ADJUSTMENT,
    RESERVED,
    RELEASED
}
