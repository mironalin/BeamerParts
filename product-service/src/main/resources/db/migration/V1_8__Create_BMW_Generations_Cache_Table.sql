-- Product Service - Create BMW Generations Cache Table
-- Cached BMW generation data for fast compatibility queries

CREATE TABLE bmw_generations_cache (
    code VARCHAR(20) PRIMARY KEY, -- 'F30', 'E90', etc.
    series_code VARCHAR(10) NOT NULL, -- References bmw_series_cache(code)
    name VARCHAR(100) NOT NULL,
    year_start INTEGER NOT NULL,
    year_end INTEGER,
    body_codes TEXT[],
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_generations_cache_series FOREIGN KEY (series_code) REFERENCES bmw_series_cache(code)
);

-- Create indexes for performance (per Phase 1 documentation)
CREATE INDEX idx_bmw_generations_cache_code ON bmw_generations_cache(code);
CREATE INDEX idx_bmw_generations_cache_series ON bmw_generations_cache(series_code);
