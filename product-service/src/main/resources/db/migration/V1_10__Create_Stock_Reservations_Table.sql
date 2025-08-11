-- Stock Reservations Table
-- Manages temporary stock holds for cart items and orders

CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    reservation_id VARCHAR(50) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    quantity_reserved INTEGER NOT NULL CHECK (quantity_reserved > 0),
    user_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(50),
    source VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    CONSTRAINT fk_stock_reservations_product 
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_reservations_variant 
        FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_stock_reservations_reservation_id ON stock_reservations(reservation_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_variant_id ON stock_reservations(variant_id);
CREATE INDEX idx_stock_reservations_user_id ON stock_reservations(user_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations(expires_at);
CREATE INDEX idx_stock_reservations_active_expires ON stock_reservations(is_active, expires_at);

-- Composite index for efficient cleanup of expired reservations
CREATE INDEX idx_stock_reservations_cleanup ON stock_reservations(is_active, expires_at) 
    WHERE is_active = true;
