package live.alinmiron.beamerparts.vehicle.repository;

import live.alinmiron.beamerparts.vehicle.entity.BmwSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BMW Series repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface BmwSeriesRepository extends JpaRepository<BmwSeries, Long> {
    
    // Uses idx_bmw_series_code index
    Optional<BmwSeries> findByCode(String code);
    
    // Uses idx_bmw_series_code index
    boolean existsByCode(String code);
    
    // Uses idx_bmw_series_active index
    List<BmwSeries> findByIsActive(Boolean isActive);
    
    // Find active series ordered by display order
    @Query("SELECT s FROM BmwSeries s WHERE s.isActive = true ORDER BY s.displayOrder")
    List<BmwSeries> findAllActiveOrderByDisplayOrder();
    
    // Find series with active generations
    @Query("SELECT DISTINCT s FROM BmwSeries s " +
           "JOIN s.generations g WHERE s.isActive = true AND g.isActive = true " +
           "ORDER BY s.displayOrder")
    List<BmwSeries> findSeriesWithActiveGenerations();
    
    // Find by name (case-insensitive)
    @Query("SELECT s FROM BmwSeries s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BmwSeries> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Count active series
    long countByIsActive(Boolean isActive);
    
    // Check if code exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM BmwSeries s WHERE s.code = :code AND s.id != :id")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("id") Long id);
    
    // Find series by codes (bulk operation)
    @Query("SELECT s FROM BmwSeries s WHERE s.code IN :codes")
    List<BmwSeries> findByCodeIn(@Param("codes") List<String> codes);
}
