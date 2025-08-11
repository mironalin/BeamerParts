package live.alinmiron.beamerparts.vehicle.repository;

import live.alinmiron.beamerparts.vehicle.entity.BmwGeneration;
import live.alinmiron.beamerparts.vehicle.entity.BmwSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BMW Generation repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface BmwGenerationRepository extends JpaRepository<BmwGeneration, Long> {
    
    // Uses idx_bmw_generations_code index
    Optional<BmwGeneration> findByCode(String code);
    
    // Uses idx_bmw_generations_code index
    boolean existsByCode(String code);
    
    // Uses idx_bmw_generations_series index
    List<BmwGeneration> findBySeries(BmwSeries series);
    
    // Uses idx_bmw_generations_series index
    List<BmwGeneration> findBySeriesId(Long seriesId);
    
    // Uses idx_bmw_generations_active index
    List<BmwGeneration> findByIsActive(Boolean isActive);
    
    // Find active generations for a series
    @Query("SELECT g FROM BmwGeneration g WHERE g.series = :series AND g.isActive = true " +
           "ORDER BY g.yearStart DESC")
    List<BmwGeneration> findActiveGenerationsBySeries(@Param("series") BmwSeries series);
    
    // Find active generations by series code
    @Query("SELECT g FROM BmwGeneration g WHERE g.series.code = :seriesCode AND g.isActive = true " +
           "ORDER BY g.yearStart DESC")
    List<BmwGeneration> findActiveGenerationsBySeriesCode(@Param("seriesCode") String seriesCode);
    
    // Find current generation (no end year)
    @Query("SELECT g FROM BmwGeneration g WHERE g.yearEnd IS NULL AND g.isActive = true")
    List<BmwGeneration> findCurrentGenerations();
    
    // Find generation by year range
    @Query("SELECT g FROM BmwGeneration g WHERE g.yearStart <= :year " +
           "AND (g.yearEnd IS NULL OR g.yearEnd >= :year) AND g.isActive = true")
    List<BmwGeneration> findByYear(@Param("year") Integer year);
    
    // Find generations by body code
    @Query(value = "SELECT * FROM bmw_generations g WHERE :bodyCode = ANY(g.body_codes) AND g.is_active = true", 
           nativeQuery = true)
    List<BmwGeneration> findByBodyCode(@Param("bodyCode") String bodyCode);

    // Check if code exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END " +
        "FROM BmwGeneration g WHERE g.code = :code AND g.id != :id")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("id") Long id);

    // Find generations by codes (bulk operation)
    @Query("SELECT g FROM BmwGeneration g WHERE g.code IN :codes")
    List<BmwGeneration> findByCodeIn(@Param("codes") List<String> codes);
    
    // Count generations by series
    long countBySeries(BmwSeries series);
    
    // Count active generations by series
    @Query("SELECT COUNT(g) FROM BmwGeneration g WHERE g.series = :series AND g.isActive = true")
    long countActiveGenerationsBySeries(@Param("series") BmwSeries series);
    
    // Find overlapping generations (for validation)
    @Query("SELECT g FROM BmwGeneration g WHERE g.series = :series " +
           "AND g.id != :excludeId " +
           "AND ((g.yearStart BETWEEN :startYear AND :endYear) " +
           "OR (g.yearEnd BETWEEN :startYear AND :endYear) " +
           "OR (g.yearStart <= :startYear AND (g.yearEnd IS NULL OR g.yearEnd >= :endYear)))")
    List<BmwGeneration> findOverlappingGenerations(@Param("series") BmwSeries series,
                                                  @Param("excludeId") Long excludeId,
                                                  @Param("startYear") Integer startYear,
                                                  @Param("endYear") Integer endYear);
}
