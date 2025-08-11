-- Product Service - Create BMW Series Cache Table
-- Cached BMW data for fast compatibility queries

CREATE TABLE bmw_series_cache (
    code VARCHAR(10) PRIMARY KEY, -- '3', 'X5', etc.
    name VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance (per Phase 1 documentation)
CREATE INDEX idx_bmw_series_cache_code ON bmw_series_cache(code);
