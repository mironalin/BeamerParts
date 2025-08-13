package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain service that encapsulates all BMW compatibility and cache management business logic.
 * 
 * Business Rules:
 * - BMW data synchronization from Vehicle Service maintains hierarchy integrity
 * - Series must exist before generations can be created
 * - Cache updates preserve data consistency and audit trails
 * - Only active BMW data is available for product compatibility
 * - Bulk operations maintain transactional integrity
 * - Business validation prevents invalid BMW hierarchy relationships
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BmwCompatibilityDomainService {

    private final BmwSeriesCacheRepository bmwSeriesCacheRepository;
    private final BmwGenerationCacheRepository bmwGenerationCacheRepository;

    /**
     * Synchronize BMW series data from Vehicle Service with business validation.
     * 
     * @param seriesCode BMW series code (e.g., "3", "X5")
     * @param name Series name (e.g., "3 Series")
     * @param displayOrder Display priority order
     * @param isActive Whether series is currently active
     * @return BmwSeriesSyncResult with success status and details
     */
    public BmwSeriesSyncResult synchronizeSeries(String seriesCode, String name, Integer displayOrder, Boolean isActive) {
        log.debug("Synchronizing BMW series: {} - {}", seriesCode, name);
        
        // Business validation
        if (seriesCode == null || seriesCode.trim().isEmpty()) {
            return BmwSeriesSyncResult.failure("Series code cannot be empty");
        }
        
        if (name == null || name.trim().isEmpty()) {
            return BmwSeriesSyncResult.failure("Series name cannot be empty");
        }
        
        if (displayOrder == null || displayOrder < 0) {
            return BmwSeriesSyncResult.failure("Display order must be a positive number");
        }
        
        try {
            // Find existing or create new series
            BmwSeriesCache series = bmwSeriesCacheRepository.findByCode(seriesCode.trim())
                    .orElse(BmwSeriesCache.builder()
                            .code(seriesCode.trim())
                            .build());
            
            // Apply business rules for series updates
            series.setName(name.trim());
            series.setDisplayOrder(displayOrder);
            series.setIsActive(isActive != null ? isActive : true);
            series.setLastUpdated(LocalDateTime.now());
            
            BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);
            
            log.info("BMW series synchronized successfully: {} - {}", seriesCode, name);
            return BmwSeriesSyncResult.success(savedSeries.getCode());
            
        } catch (Exception e) {
            log.error("Failed to synchronize BMW series {}: {}", seriesCode, e.getMessage());
            return BmwSeriesSyncResult.failure("Synchronization failed: " + e.getMessage());
        }
    }

    /**
     * Synchronize BMW generation data from Vehicle Service with parent series validation.
     * 
     * @param generationCode BMW generation code (e.g., "F30", "G20")
     * @param seriesCode Parent BMW series code
     * @param name Generation name
     * @param yearStart Production start year
     * @param yearEnd Production end year (null for current generation)
     * @param bodyCodes List of body style codes
     * @param isActive Whether generation is currently active
     * @return BmwGenerationSyncResult with success status and details
     */
    public BmwGenerationSyncResult synchronizeGeneration(String generationCode, String seriesCode, String name, 
                                                        Integer yearStart, Integer yearEnd, List<String> bodyCodes, Boolean isActive) {
        log.debug("Synchronizing BMW generation: {} for series {}", generationCode, seriesCode);
        
        // Business validation
        if (generationCode == null || generationCode.trim().isEmpty()) {
            return BmwGenerationSyncResult.failure("Generation code cannot be empty");
        }
        
        if (name == null || name.trim().isEmpty()) {
            return BmwGenerationSyncResult.failure("Generation name cannot be empty");
        }
        
        if (yearStart == null || yearStart < 1900 || yearStart > LocalDateTime.now().getYear() + 5) {
            return BmwGenerationSyncResult.failure("Year start must be a valid year");
        }
        
        if (yearEnd != null && yearEnd < yearStart) {
            return BmwGenerationSyncResult.failure("Year end cannot be before year start");
        }
        
        try {
            // Validate parent series exists (business rule: series must exist before generations)
            Optional<BmwSeriesCache> parentSeriesOpt = bmwSeriesCacheRepository.findByCode(seriesCode);
            if (parentSeriesOpt.isEmpty()) {
                return BmwGenerationSyncResult.failure("Parent series not found: " + seriesCode);
            }
            
            BmwSeriesCache parentSeries = parentSeriesOpt.get();
            
            // Find existing or create new generation
            BmwGenerationCache generation = bmwGenerationCacheRepository.findByCode(generationCode.trim())
                    .orElse(BmwGenerationCache.builder()
                            .code(generationCode.trim())
                            .build());
            
            // Apply business rules for generation updates
            generation.setSeriesCache(parentSeries);
            generation.setName(name.trim());
            generation.setYearStart(yearStart);
            generation.setYearEnd(yearEnd);
            generation.setBodyCodes(bodyCodes != null && !bodyCodes.isEmpty() ? 
                    bodyCodes.toArray(new String[0]) : null);
            generation.setIsActive(isActive != null ? isActive : true);
            generation.setLastUpdated(LocalDateTime.now());
            
            BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);
            
            log.info("BMW generation synchronized successfully: {} for series {}", generationCode, seriesCode);
            return BmwGenerationSyncResult.success(savedGeneration.getCode(), savedGeneration.getSeriesCache().getCode());
            
        } catch (Exception e) {
            log.error("Failed to synchronize BMW generation {}: {}", generationCode, e.getMessage());
            return BmwGenerationSyncResult.failure("Synchronization failed: " + e.getMessage());
        }
    }

    /**
     * Retrieve all active BMW series ordered by display priority.
     * 
     * @return List of active BMW series information
     */
    @Transactional(readOnly = true)
    public List<BmwSeriesInfo> getAllActiveSeries() {
        log.debug("Retrieving all active BMW series");
        
        List<BmwSeriesCache> activeSeries = bmwSeriesCacheRepository.findAllActiveOrderByDisplayOrder();
        
        return activeSeries.stream()
                .map(this::mapSeriesToInfo)
                .toList();
    }

    /**
     * Retrieve BMW generations for specific series with hierarchical context.
     * 
     * @param seriesCode BMW series code
     * @return List of generations for the specified series
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationInfo> getGenerationsForSeries(String seriesCode) {
        log.debug("Retrieving generations for BMW series: {}", seriesCode);
        
        if (seriesCode == null || seriesCode.trim().isEmpty()) {
            log.warn("Invalid series code provided: {}", seriesCode);
            return List.of();
        }
        
        List<BmwGenerationCache> generations = bmwGenerationCacheRepository.findBySeriesCode(seriesCode.trim());
        
        return generations.stream()
                .map(this::mapGenerationToInfo)
                .toList();
    }

    /**
     * Validate if BMW generation exists and is valid for product compatibility.
     * 
     * @param generationCode BMW generation code
     * @return true if generation exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isGenerationValidForCompatibility(String generationCode) {
        if (generationCode == null || generationCode.trim().isEmpty()) {
            return false;
        }
        
        return bmwGenerationCacheRepository.findByCode(generationCode.trim())
                .map(BmwGenerationCache::getIsActive)
                .orElse(false);
    }

    /**
     * Perform bulk synchronization of BMW data with transactional integrity.
     * 
     * @param seriesData List of BMW series data to synchronize
     * @param generationData List of BMW generation data to synchronize
     * @return BulkSyncResult with operation status and statistics
     */
    public BulkSyncResult performBulkSync(List<BmwSeriesSyncData> seriesData, List<BmwGenerationSyncData> generationData) {
        log.info("Starting bulk BMW data synchronization: {} series, {} generations", 
                seriesData.size(), generationData.size());
        
        try {
            int seriesSynced = 0;
            int generationsSynced = 0;
            
            // Synchronize series first (business rule: series before generations)
            for (BmwSeriesSyncData series : seriesData) {
                BmwSeriesSyncResult result = synchronizeSeries(
                    series.getCode(), series.getName(), series.getDisplayOrder(), series.getIsActive());
                
                if (result.isSuccess()) {
                    seriesSynced++;
                } else {
                    log.warn("Failed to sync series {}: {}", series.getCode(), result.getErrorMessage());
                }
            }
            
            // Then synchronize generations
            for (BmwGenerationSyncData generation : generationData) {
                BmwGenerationSyncResult result = synchronizeGeneration(
                    generation.getCode(), generation.getSeriesCode(), generation.getName(),
                    generation.getYearStart(), generation.getYearEnd(), generation.getBodyCodes(), generation.getIsActive());
                
                if (result.isSuccess()) {
                    generationsSynced++;
                } else {
                    log.warn("Failed to sync generation {}: {}", generation.getCode(), result.getErrorMessage());
                }
            }
            
            log.info("Bulk synchronization completed: {} series, {} generations", seriesSynced, generationsSynced);
            return BulkSyncResult.success(seriesSynced, generationsSynced);
            
        } catch (Exception e) {
            log.error("Bulk synchronization failed: {}", e.getMessage());
            return BulkSyncResult.failure("Bulk synchronization failed: " + e.getMessage());
        }
    }

    // === BMW Series Query Operations ===

    /**
     * Get all BMW series from cache
     */
    @Transactional(readOnly = true)
    public List<BmwSeriesCache> findAllBmwSeries() {
        log.debug("Finding all BMW series from cache");
        return bmwSeriesCacheRepository.findAll();
    }

    /**
     * Get active BMW series from cache ordered by display order
     */
    @Transactional(readOnly = true)
    public List<BmwSeriesCache> findActiveBmwSeries() {
        log.debug("Finding active BMW series from cache");
        return bmwSeriesCacheRepository.findAllActiveOrderByDisplayOrder();
    }

    /**
     * Get BMW series by code
     */
    @Transactional(readOnly = true)
    public Optional<BmwSeriesCache> findBmwSeriesByCode(String seriesCode) {
        log.debug("Finding BMW series by code: {}", seriesCode);
        return bmwSeriesCacheRepository.findByCode(seriesCode);
    }

    /**
     * Search BMW series by name (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<BmwSeriesCache> searchBmwSeriesByName(String name) {
        log.debug("Searching BMW series by name: {}", name);
        return bmwSeriesCacheRepository.findByNameContainingIgnoreCase(name);
    }

    // === BMW Generation Query Operations ===

    /**
     * Get all BMW generations from cache
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationCache> findAllBmwGenerations() {
        log.debug("Finding all BMW generations from cache");
        return bmwGenerationCacheRepository.findAll();
    }

    /**
     * Get active BMW generations from cache ordered by year desc
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationCache> findActiveBmwGenerations() {
        log.debug("Finding active BMW generations from cache");
        return bmwGenerationCacheRepository.findAllActiveOrderByYearDesc();
    }

    /**
     * Get BMW generation by code
     */
    @Transactional(readOnly = true)
    public Optional<BmwGenerationCache> findBmwGenerationByCode(String generationCode) {
        log.debug("Finding BMW generation by code: {}", generationCode);
        return bmwGenerationCacheRepository.findByCode(generationCode);
    }

    /**
     * Get BMW generations by series code
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationCache> findBmwGenerationsBySeriesCode(String seriesCode) {
        log.debug("Finding BMW generations for series: {}", seriesCode);
        return bmwGenerationCacheRepository.findBySeriesCode(seriesCode);
    }

    /**
     * Map BmwSeriesCache entity to business information object.
     */
    private BmwSeriesInfo mapSeriesToInfo(BmwSeriesCache series) {
        return BmwSeriesInfo.builder()
                .code(series.getCode())
                .name(series.getName())
                .displayName(series.getDisplayName())
                .displayOrder(series.getDisplayOrder())
                .isActive(series.getIsActive())
                .lastUpdated(series.getLastUpdated())
                .build();
    }

    /**
     * Map BmwGenerationCache entity to business information object.
     */
    private BmwGenerationInfo mapGenerationToInfo(BmwGenerationCache generation) {
        return BmwGenerationInfo.builder()
                .code(generation.getCode())
                .seriesCode(generation.getSeriesCache().getCode())
                .seriesName(generation.getSeriesCache().getName())
                .name(generation.getName())
                .displayName(generation.getDisplayName())
                .yearStart(generation.getYearStart())
                .yearEnd(generation.getYearEnd())
                .yearRange(generation.getYearRange())
                .bodyCodes(generation.getBodyCodes() != null ? List.of(generation.getBodyCodes()) : List.of())
                .isCurrentGeneration(generation.isCurrentGeneration())
                .isActive(generation.getIsActive())
                .lastUpdated(generation.getLastUpdated())
                .build();
    }

    // Domain Result Classes
    
    public static class BmwSeriesSyncResult {
        private final boolean success;
        private final String seriesCode;
        private final String errorMessage;

        private BmwSeriesSyncResult(boolean success, String seriesCode, String errorMessage) {
            this.success = success;
            this.seriesCode = seriesCode;
            this.errorMessage = errorMessage;
        }

        public static BmwSeriesSyncResult success(String seriesCode) {
            return new BmwSeriesSyncResult(true, seriesCode, null);
        }

        public static BmwSeriesSyncResult failure(String errorMessage) {
            return new BmwSeriesSyncResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getSeriesCode() { return seriesCode; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class BmwGenerationSyncResult {
        private final boolean success;
        private final String generationCode;
        private final String seriesCode;
        private final String errorMessage;

        private BmwGenerationSyncResult(boolean success, String generationCode, String seriesCode, String errorMessage) {
            this.success = success;
            this.generationCode = generationCode;
            this.seriesCode = seriesCode;
            this.errorMessage = errorMessage;
        }

        public static BmwGenerationSyncResult success(String generationCode, String seriesCode) {
            return new BmwGenerationSyncResult(true, generationCode, seriesCode, null);
        }

        public static BmwGenerationSyncResult failure(String errorMessage) {
            return new BmwGenerationSyncResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getGenerationCode() { return generationCode; }
        public String getSeriesCode() { return seriesCode; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class BulkSyncResult {
        private final boolean success;
        private final int seriesSynced;
        private final int generationsSynced;
        private final String errorMessage;

        private BulkSyncResult(boolean success, int seriesSynced, int generationsSynced, String errorMessage) {
            this.success = success;
            this.seriesSynced = seriesSynced;
            this.generationsSynced = generationsSynced;
            this.errorMessage = errorMessage;
        }

        public static BulkSyncResult success(int seriesSynced, int generationsSynced) {
            return new BulkSyncResult(true, seriesSynced, generationsSynced, null);
        }

        public static BulkSyncResult failure(String errorMessage) {
            return new BulkSyncResult(false, 0, 0, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public int getSeriesSynced() { return seriesSynced; }
        public int getGenerationsSynced() { return generationsSynced; }
        public String getErrorMessage() { return errorMessage; }
    }

    // Domain Data Classes

    public static class BmwSeriesSyncData {
        private final String code;
        private final String name;
        private final Integer displayOrder;
        private final Boolean isActive;

        public BmwSeriesSyncData(String code, String name, Integer displayOrder, Boolean isActive) {
            this.code = code;
            this.name = name;
            this.displayOrder = displayOrder;
            this.isActive = isActive;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public Boolean getIsActive() { return isActive; }
    }

    public static class BmwGenerationSyncData {
        private final String code;
        private final String seriesCode;
        private final String name;
        private final Integer yearStart;
        private final Integer yearEnd;
        private final List<String> bodyCodes;
        private final Boolean isActive;

        public BmwGenerationSyncData(String code, String seriesCode, String name, Integer yearStart, 
                                    Integer yearEnd, List<String> bodyCodes, Boolean isActive) {
            this.code = code;
            this.seriesCode = seriesCode;
            this.name = name;
            this.yearStart = yearStart;
            this.yearEnd = yearEnd;
            this.bodyCodes = bodyCodes;
            this.isActive = isActive;
        }

        public String getCode() { return code; }
        public String getSeriesCode() { return seriesCode; }
        public String getName() { return name; }
        public Integer getYearStart() { return yearStart; }
        public Integer getYearEnd() { return yearEnd; }
        public List<String> getBodyCodes() { return bodyCodes; }
        public Boolean getIsActive() { return isActive; }
    }

    // Domain Information Classes

    public static class BmwSeriesInfo {
        private final String code;
        private final String name;
        private final String displayName;
        private final Integer displayOrder;
        private final Boolean isActive;
        private final LocalDateTime lastUpdated;

        private BmwSeriesInfo(String code, String name, String displayName, Integer displayOrder, 
                             Boolean isActive, LocalDateTime lastUpdated) {
            this.code = code;
            this.name = name;
            this.displayName = displayName;
            this.displayOrder = displayOrder;
            this.isActive = isActive;
            this.lastUpdated = lastUpdated;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String code;
            private String name;
            private String displayName;
            private Integer displayOrder;
            private Boolean isActive;
            private LocalDateTime lastUpdated;

            public Builder code(String code) { this.code = code; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder displayName(String displayName) { this.displayName = displayName; return this; }
            public Builder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
            public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
            public Builder lastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; return this; }

            public BmwSeriesInfo build() {
                return new BmwSeriesInfo(code, name, displayName, displayOrder, isActive, lastUpdated);
            }
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public Integer getDisplayOrder() { return displayOrder; }
        public Boolean getIsActive() { return isActive; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    public static class BmwGenerationInfo {
        private final String code;
        private final String seriesCode;
        private final String seriesName;
        private final String name;
        private final String displayName;
        private final Integer yearStart;
        private final Integer yearEnd;
        private final String yearRange;
        private final List<String> bodyCodes;
        private final Boolean isCurrentGeneration;
        private final Boolean isActive;
        private final LocalDateTime lastUpdated;

        private BmwGenerationInfo(String code, String seriesCode, String seriesName, String name, String displayName,
                                 Integer yearStart, Integer yearEnd, String yearRange, List<String> bodyCodes,
                                 Boolean isCurrentGeneration, Boolean isActive, LocalDateTime lastUpdated) {
            this.code = code;
            this.seriesCode = seriesCode;
            this.seriesName = seriesName;
            this.name = name;
            this.displayName = displayName;
            this.yearStart = yearStart;
            this.yearEnd = yearEnd;
            this.yearRange = yearRange;
            this.bodyCodes = bodyCodes;
            this.isCurrentGeneration = isCurrentGeneration;
            this.isActive = isActive;
            this.lastUpdated = lastUpdated;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String code;
            private String seriesCode;
            private String seriesName;
            private String name;
            private String displayName;
            private Integer yearStart;
            private Integer yearEnd;
            private String yearRange;
            private List<String> bodyCodes;
            private Boolean isCurrentGeneration;
            private Boolean isActive;
            private LocalDateTime lastUpdated;

            public Builder code(String code) { this.code = code; return this; }
            public Builder seriesCode(String seriesCode) { this.seriesCode = seriesCode; return this; }
            public Builder seriesName(String seriesName) { this.seriesName = seriesName; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder displayName(String displayName) { this.displayName = displayName; return this; }
            public Builder yearStart(Integer yearStart) { this.yearStart = yearStart; return this; }
            public Builder yearEnd(Integer yearEnd) { this.yearEnd = yearEnd; return this; }
            public Builder yearRange(String yearRange) { this.yearRange = yearRange; return this; }
            public Builder bodyCodes(List<String> bodyCodes) { this.bodyCodes = bodyCodes; return this; }
            public Builder isCurrentGeneration(Boolean isCurrentGeneration) { this.isCurrentGeneration = isCurrentGeneration; return this; }
            public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
            public Builder lastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; return this; }

            public BmwGenerationInfo build() {
                return new BmwGenerationInfo(code, seriesCode, seriesName, name, displayName, yearStart, yearEnd, 
                                           yearRange, bodyCodes, isCurrentGeneration, isActive, lastUpdated);
            }
        }

        public String getCode() { return code; }
        public String getSeriesCode() { return seriesCode; }
        public String getSeriesName() { return seriesName; }
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public Integer getYearStart() { return yearStart; }
        public Integer getYearEnd() { return yearEnd; }
        public String getYearRange() { return yearRange; }
        public List<String> getBodyCodes() { return bodyCodes; }
        public Boolean isCurrentGeneration() { return isCurrentGeneration; }
        public Boolean getIsActive() { return isActive; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
}
