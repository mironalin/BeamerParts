package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BmwGenerationCache repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface BmwGenerationCacheRepository extends JpaRepository<BmwGenerationCache, String> {
    
    // Uses idx_bmw_generations_cache_code index
    Optional<BmwGenerationCache> findByCode(String code);
    
    // Uses idx_bmw_generations_cache_code index
    boolean existsByCode(String code);
    
    // Uses idx_bmw_generations_cache_series index
    List<BmwGenerationCache> findBySeriesCache(BmwSeriesCache seriesCache);
    
    // Find by series code
    @Query("SELECT g FROM BmwGenerationCache g WHERE g.seriesCache.code = :seriesCode")
    List<BmwGenerationCache> findBySeriesCode(@Param("seriesCode") String seriesCode);
    
    // Find active generations
    List<BmwGenerationCache> findByIsActive(Boolean isActive);
    
    // Find active generations by series
    List<BmwGenerationCache> findBySeriesCacheAndIsActive(BmwSeriesCache seriesCache, Boolean isActive);
    
    // Find active generations by series code
    @Query("SELECT g FROM BmwGenerationCache g WHERE g.seriesCache.code = :seriesCode AND g.isActive = :isActive")
    List<BmwGenerationCache> findBySeriesCodeAndIsActive(@Param("seriesCode") String seriesCode, @Param("isActive") Boolean isActive);
    
    // Find current generations (yearEnd is null)
    @Query("SELECT g FROM BmwGenerationCache g WHERE g.yearEnd IS NULL AND g.isActive = true")
    List<BmwGenerationCache> findCurrentGenerations();
    
    // Find generations by year range
    @Query("SELECT g FROM BmwGenerationCache g WHERE " +
           "(:year >= g.yearStart) AND (:year <= g.yearEnd OR g.yearEnd IS NULL) " +
           "AND g.isActive = true")
    List<BmwGenerationCache> findByYear(@Param("year") Integer year);
    
    // Find generations by year range for specific series
    @Query("SELECT g FROM BmwGenerationCache g WHERE " +
           "g.seriesCache.code = :seriesCode AND " +
           "(:year >= g.yearStart) AND (:year <= g.yearEnd OR g.yearEnd IS NULL) " +
           "AND g.isActive = true")
    List<BmwGenerationCache> findBySeriesCodeAndYear(@Param("seriesCode") String seriesCode, @Param("year") Integer year);
    
    // Find by name (case-insensitive)
    @Query("SELECT g FROM BmwGenerationCache g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BmwGenerationCache> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find by body code
    @Query(value = "SELECT * FROM bmw_generations g WHERE :bodyCode = ANY(g.body_codes) AND g.is_active = true", 
           nativeQuery = true)
    List<BmwGenerationCache> findByBodyCode(@Param("bodyCode") String bodyCode);
    
    // Count generations by series
    long countBySeriesCache(BmwSeriesCache seriesCache);
    
    // Count active generations by series
    long countBySeriesCacheAndIsActive(BmwSeriesCache seriesCache, Boolean isActive);
    
    // Check if code exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END " +
           "FROM BmwGenerationCache g WHERE g.code = :code AND g.code != :excludeCode")
    boolean existsByCodeAndCodeNot(@Param("code") String code, @Param("excludeCode") String excludeCode);
    
    // Find generations by codes (bulk operation)
    @Query("SELECT g FROM BmwGenerationCache g WHERE g.code IN :codes")
    List<BmwGenerationCache> findByCodes(@Param("codes") List<String> codes);
    
    // Find most compatible generations (by product count)
    @Query("SELECT g FROM BmwGenerationCache g " +
           "JOIN g.compatibilityEntries pc " +
           "WHERE g.isActive = true " +
           "GROUP BY g " +
           "ORDER BY COUNT(pc) DESC")
    List<BmwGenerationCache> findMostCompatibleGenerations();
    
    // Find generations ordered by year (newest first)
    @Query("SELECT g FROM BmwGenerationCache g WHERE g.isActive = true " +
           "ORDER BY g.yearStart DESC, g.yearEnd DESC NULLS FIRST")
    List<BmwGenerationCache> findAllActiveOrderByYearDesc();
}
