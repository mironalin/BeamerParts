package live.alinmiron.beamerparts.product.service.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BmwSeriesSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwGenerationSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwSeriesCacheDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwGenerationCacheDto;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import live.alinmiron.beamerparts.product.service.domain.BmwCompatibilityDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal BMW Cache Service for managing BMW hierarchy data cache
 * Thin orchestration layer that delegates business logic to BmwCompatibilityDomainService
 * 
 * This service handles:
 * - DTO mapping and transformation
 * - API contract fulfillment for Vehicle Service synchronization
 * - Coordination between domain services and external systems
 * 
 * Business logic is handled by BmwCompatibilityDomainService following DDD principles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BmwCacheInternalService {
    
    private final BmwCompatibilityDomainService bmwCompatibilityDomainService;
    private final BmwSeriesCacheRepository bmwSeriesCacheRepository;
    private final BmwGenerationCacheRepository bmwGenerationCacheRepository;
    
    /**
     * Sync BMW series data from Vehicle Service (delegates to domain service)
     */
    public BmwSeriesCacheDto syncSeries(BmwSeriesSyncRequestDto request) {
        log.debug("Syncing BMW series: {} - {}", request.getCode(), request.getName());
        
        // Delegate to domain service for business logic and validation
        BmwCompatibilityDomainService.BmwSeriesSyncResult syncResult = 
            bmwCompatibilityDomainService.synchronizeSeries(
                request.getCode(), 
                request.getName(), 
                request.getDisplayOrder(), 
                request.getIsActive());
        
        if (!syncResult.isSuccess()) {
            throw new RuntimeException("Failed to sync BMW series: " + syncResult.getErrorMessage());
        }
        
        // Retrieve and return the synchronized series
        BmwSeriesCache syncedSeries = bmwSeriesCacheRepository.findByCode(syncResult.getSeriesCode())
                .orElseThrow(() -> new RuntimeException("Series not found after sync: " + syncResult.getSeriesCode()));
        
        return mapSeriesToDto(syncedSeries);
    }
    
    /**
     * Sync BMW generation data from Vehicle Service (delegates to domain service)
     */
    public BmwGenerationCacheDto syncGeneration(BmwGenerationSyncRequestDto request) {
        log.debug("Syncing BMW generation: {} - {}", request.getCode(), request.getName());
        
        // Delegate to domain service for business logic and validation
        BmwCompatibilityDomainService.BmwGenerationSyncResult syncResult = 
            bmwCompatibilityDomainService.synchronizeGeneration(
                request.getCode(),
                request.getSeriesCode(),
                request.getName(),
                request.getYearStart(),
                request.getYearEnd(),
                request.getBodyCodes(),
                request.getIsActive());
        
        if (!syncResult.isSuccess()) {
            throw new RuntimeException("Failed to sync BMW generation: " + syncResult.getErrorMessage());
        }
        
        // Retrieve and return the synchronized generation
        BmwGenerationCache syncedGeneration = bmwGenerationCacheRepository.findByCode(syncResult.getGenerationCode())
                .orElseThrow(() -> new RuntimeException("Generation not found after sync: " + syncResult.getGenerationCode()));
        
        return mapGenerationToDto(syncedGeneration);
    }
    
    /**
     * Get all BMW series from cache
     */
    @Transactional(readOnly = true)
    public List<BmwSeriesCacheDto> getAllSeries() {
        List<BmwSeriesCache> series = bmwSeriesCacheRepository.findAllActiveOrderByDisplayOrder();
        return series.stream()
                .map(this::mapSeriesToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get BMW series by code
     */
    @Transactional(readOnly = true)
    public BmwSeriesCacheDto getSeriesByCode(String code) {
        return bmwSeriesCacheRepository.findByCode(code)
                .map(this::mapSeriesToDto)
                .orElseThrow(() -> new RuntimeException("BMW series not found: " + code));
    }
    
    /**
     * Get all BMW generations from cache
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationCacheDto> getAllGenerations() {
        List<BmwGenerationCache> generations = bmwGenerationCacheRepository.findAllActiveOrderByYearDesc();
        return generations.stream()
                .map(this::mapGenerationToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get BMW generation by code
     */
    @Transactional(readOnly = true)
    public BmwGenerationCacheDto getGenerationByCode(String code) {
        return bmwGenerationCacheRepository.findByCode(code)
                .map(this::mapGenerationToDto)
                .orElseThrow(() -> new RuntimeException("BMW generation not found: " + code));
    }
    
    /**
     * Get generations for a specific series
     */
    @Transactional(readOnly = true)
    public List<BmwGenerationCacheDto> getGenerationsBySeriesCode(String seriesCode) {
        List<BmwGenerationCache> generations = bmwGenerationCacheRepository.findBySeriesCode(seriesCode);
        return generations.stream()
                .map(this::mapGenerationToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Bulk sync operation for initial cache population (delegates to domain service)
     */
    public void bulkSync(List<BmwSeriesSyncRequestDto> seriesData, 
                        List<BmwGenerationSyncRequestDto> generationData) {
        log.info("Starting bulk sync: {} series, {} generations", 
                seriesData.size(), generationData.size());
        
        // Convert DTOs to domain data objects
        List<BmwCompatibilityDomainService.BmwSeriesSyncData> domainSeriesData = seriesData.stream()
                .map(dto -> new BmwCompatibilityDomainService.BmwSeriesSyncData(
                    dto.getCode(), dto.getName(), dto.getDisplayOrder(), dto.getIsActive()))
                .toList();
        
        List<BmwCompatibilityDomainService.BmwGenerationSyncData> domainGenerationData = generationData.stream()
                .map(dto -> new BmwCompatibilityDomainService.BmwGenerationSyncData(
                    dto.getCode(), dto.getSeriesCode(), dto.getName(), 
                    dto.getYearStart(), dto.getYearEnd(), dto.getBodyCodes(), dto.getIsActive()))
                .toList();
        
        // Delegate to domain service for transactional bulk operations
        BmwCompatibilityDomainService.BulkSyncResult bulkResult = 
            bmwCompatibilityDomainService.performBulkSync(domainSeriesData, domainGenerationData);
        
        if (!bulkResult.isSuccess()) {
            log.error("Bulk sync failed: {}", bulkResult.getErrorMessage());
            throw new RuntimeException("Bulk sync failed: " + bulkResult.getErrorMessage());
        }
        
        log.info("Bulk sync completed successfully: {} series, {} generations synced", 
                bulkResult.getSeriesSynced(), bulkResult.getGenerationsSynced());
    }
    
    /**
     * Map BmwSeriesCache entity to DTO
     */
    private BmwSeriesCacheDto mapSeriesToDto(BmwSeriesCache series) {
        return BmwSeriesCacheDto.builder()
                .code(series.getCode())
                .name(series.getName())
                .displayOrder(series.getDisplayOrder())
                .isActive(series.getIsActive())
                .lastUpdated(series.getLastUpdated())
                .build();
    }
    
    /**
     * Map BmwGenerationCache entity to DTO
     */
    private BmwGenerationCacheDto mapGenerationToDto(BmwGenerationCache generation) {
        return BmwGenerationCacheDto.builder()
                .code(generation.getCode())
                .seriesCode(generation.getSeriesCache().getCode())
                .name(generation.getName())
                .yearStart(generation.getYearStart())
                .yearEnd(generation.getYearEnd())
                .bodyCodes(generation.getBodyCodes() != null ? 
                        List.of(generation.getBodyCodes()) : null)
                .isActive(generation.getIsActive())
                .lastUpdated(generation.getLastUpdated())
                .build();
    }
}
