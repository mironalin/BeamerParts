package live.alinmiron.beamerparts.product.controller.internal;

import live.alinmiron.beamerparts.product.dto.internal.request.BmwSeriesSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwGenerationSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwCacheBulkSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwSeriesCacheDto;
import live.alinmiron.beamerparts.product.dto.shared.ApiResponse;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwGenerationCacheDto;
import live.alinmiron.beamerparts.product.service.internal.BmwCacheInternalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Internal REST controller for BMW cache management
 * These endpoints are NOT exposed through the API Gateway
 * Used by Vehicle Service for event-driven cache synchronization
 */
@RestController
@RequestMapping("/internal/bmw-cache")
@Tag(name = "BMW Cache Internal API", description = "Internal BMW hierarchy cache management for service-to-service communication")
public class BmwCacheInternalController {
    
    private final BmwCacheInternalService bmwCacheInternalService;
    
    public BmwCacheInternalController(BmwCacheInternalService bmwCacheInternalService) {
        this.bmwCacheInternalService = bmwCacheInternalService;
    }
    
    /**
     * Sync BMW series data from Vehicle Service
     * Used by: Vehicle Service (via RabbitMQ events)
     */
    @Operation(summary = "Sync BMW series", description = "Synchronize BMW series data from Vehicle Service")
    @PostMapping("/sync/series")
    public ResponseEntity<ApiResponse<BmwSeriesCacheDto>> syncSeries(
            @Valid @RequestBody BmwSeriesSyncRequestDto request) {
        
        BmwSeriesCacheDto updatedSeries = bmwCacheInternalService.syncSeries(request);
        
        return ResponseEntity.ok(ApiResponse.success(updatedSeries));
    }
    
    /**
     * Sync BMW generation data from Vehicle Service
     * Used by: Vehicle Service (via RabbitMQ events)
     */
    @Operation(summary = "Sync BMW generation", description = "Synchronize BMW generation data from Vehicle Service")
    @PostMapping("/sync/generations")
    public ResponseEntity<ApiResponse<BmwGenerationCacheDto>> syncGeneration(
            @Valid @RequestBody BmwGenerationSyncRequestDto request) {
        
        BmwGenerationCacheDto updatedGeneration = bmwCacheInternalService.syncGeneration(request);
        
        return ResponseEntity.ok(ApiResponse.success(updatedGeneration));
    }
    
    /**
     * Get all BMW series from cache
     * Used by: Internal services for quick reference
     */
    @Operation(summary = "Get all BMW series", description = "Retrieve all cached BMW series data")
    @GetMapping("/series")
    public ResponseEntity<ApiResponse<List<BmwSeriesCacheDto>>> getAllSeries() {
        
        List<BmwSeriesCacheDto> series = bmwCacheInternalService.getAllSeries();
        
        return ResponseEntity.ok(ApiResponse.success(series));
    }
    
    /**
     * Get BMW series by code
     * Used by: Internal services for validation
     */
    @Operation(summary = "Get BMW series by code", description = "Retrieve specific BMW series by code")
    @GetMapping("/series/{code}")
    public ResponseEntity<ApiResponse<BmwSeriesCacheDto>> getSeriesByCode(@Parameter(description = "BMW series code") @PathVariable String code) {
        
        BmwSeriesCacheDto series = bmwCacheInternalService.getSeriesByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success(series));
    }
    
    /**
     * Get all BMW generations from cache
     * Used by: Internal services for quick reference
     */
    @Operation(summary = "Get all BMW generations", description = "Retrieve all cached BMW generation data")
    @GetMapping("/generations")
    public ResponseEntity<ApiResponse<List<BmwGenerationCacheDto>>> getAllGenerations() {
        
        List<BmwGenerationCacheDto> generations = bmwCacheInternalService.getAllGenerations();
        
        return ResponseEntity.ok(ApiResponse.success(generations));
    }
    
    /**
     * Get BMW generation by code
     * Used by: Internal services for validation
     */
    @Operation(summary = "Get BMW generation by code", description = "Retrieve specific BMW generation by code")
    @GetMapping("/generations/{code}")
    public ResponseEntity<ApiResponse<BmwGenerationCacheDto>> getGenerationByCode(@Parameter(description = "BMW generation code") @PathVariable String code) {
        
        BmwGenerationCacheDto generation = bmwCacheInternalService.getGenerationByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success(generation));
    }
    
    /**
     * Get generations for a specific series
     * Used by: Internal services for hierarchical queries
     */
    @Operation(summary = "Get generations by series", description = "Retrieve all generations for a specific BMW series")
    @GetMapping("/series/{seriesCode}/generations")
    public ResponseEntity<ApiResponse<List<BmwGenerationCacheDto>>> getGenerationsBySeriesCode(
            @Parameter(description = "BMW series code") @PathVariable String seriesCode) {
        
        List<BmwGenerationCacheDto> generations = bmwCacheInternalService.getGenerationsBySeriesCode(seriesCode);
        
        return ResponseEntity.ok(ApiResponse.success(generations));
    }
    
    /**
     * Bulk sync operation for initial cache population
     * Used by: Vehicle Service (initial sync, full refresh)
     */
    @Operation(summary = "Bulk sync BMW cache", description = "Synchronize multiple BMW series and generations in batch")
    @PostMapping("/sync/bulk")
    public ResponseEntity<ApiResponse<String>> bulkSync(
            @Valid @RequestBody BmwCacheBulkSyncRequestDto request) {

        bmwCacheInternalService.bulkSync(request.getSeriesData(), request.getGenerationData());

        return ResponseEntity.ok(ApiResponse.success("Bulk sync completed successfully"));
    }
}
