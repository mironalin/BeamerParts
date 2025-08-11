package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BmwSeriesCache repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface BmwSeriesCacheRepository extends JpaRepository<BmwSeriesCache, String> {
    
    // Uses idx_bmw_series_cache_code index
    Optional<BmwSeriesCache> findByCode(String code);
    
    // Uses idx_bmw_series_cache_code index
    boolean existsByCode(String code);
    
    // Find active series
    List<BmwSeriesCache> findByIsActive(Boolean isActive);
    
    // Find active series ordered by display order
    @Query("SELECT s FROM BmwSeriesCache s WHERE s.isActive = true ORDER BY s.displayOrder")
    List<BmwSeriesCache> findAllActiveOrderByDisplayOrder();
    
    // Find series with active generations
    @Query("SELECT DISTINCT s FROM BmwSeriesCache s " +
           "JOIN s.generations g WHERE s.isActive = true AND g.isActive = true " +
           "ORDER BY s.displayOrder")
    List<BmwSeriesCache> findSeriesWithActiveGenerations();
    
    // Find by name (case-insensitive)
    @Query("SELECT s FROM BmwSeriesCache s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BmwSeriesCache> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Count active series
    long countByIsActive(Boolean isActive);
    
    // Check if code exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM BmwSeriesCache s WHERE s.code = :code AND s.code != :excludeCode")
    boolean existsByCodeAndCodeNot(@Param("code") String code, @Param("excludeCode") String excludeCode);
    
    // Find series by codes (bulk operation)
    @Query("SELECT s FROM BmwSeriesCache s WHERE s.code IN :codes")
    List<BmwSeriesCache> findByCodes(@Param("codes") List<String> codes);
    
    // Find most popular series (by product compatibility count)
    @Query("SELECT s FROM BmwSeriesCache s " +
           "JOIN s.generations g " +
           "JOIN g.compatibilityEntries pc " +
           "WHERE s.isActive = true " +
           "GROUP BY s " +
           "ORDER BY COUNT(pc) DESC")
    List<BmwSeriesCache> findMostPopularSeries();
}
