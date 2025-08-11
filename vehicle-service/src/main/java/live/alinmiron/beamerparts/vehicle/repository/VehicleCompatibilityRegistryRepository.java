package live.alinmiron.beamerparts.vehicle.repository;

import live.alinmiron.beamerparts.vehicle.entity.BmwGeneration;
import live.alinmiron.beamerparts.vehicle.entity.VehicleCompatibilityRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vehicle Compatibility Registry repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface VehicleCompatibilityRegistryRepository extends JpaRepository<VehicleCompatibilityRegistry, Long> {
    
    // Uses idx_vehicle_compatibility_generation index
    List<VehicleCompatibilityRegistry> findByGeneration(BmwGeneration generation);
    
    // Uses idx_vehicle_compatibility_generation index
    List<VehicleCompatibilityRegistry> findByGenerationId(Long generationId);
    
    // Uses idx_vehicle_compatibility_product index
    List<VehicleCompatibilityRegistry> findByProductSku(String productSku);
    
    // Find specific compatibility entry
    Optional<VehicleCompatibilityRegistry> findByGenerationAndProductSku(BmwGeneration generation, String productSku);
    
    // Find by generation ID and product SKU
    Optional<VehicleCompatibilityRegistry> findByGenerationIdAndProductSku(Long generationId, String productSku);
    
    // Find compatible products for a generation
    @Query("SELECT vcr.productSku FROM VehicleCompatibilityRegistry vcr WHERE vcr.generation = :generation")
    List<String> findProductSkusByGeneration(@Param("generation") BmwGeneration generation);
    
    // Find compatible products by generation code
    @Query("SELECT vcr.productSku FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation.code = :generationCode")
    List<String> findProductSkusByGenerationCode(@Param("generationCode") String generationCode);
    
    // Find compatible generations for a product
    @Query("SELECT vcr.generation FROM VehicleCompatibilityRegistry vcr WHERE vcr.productSku = :productSku")
    List<BmwGeneration> findGenerationsByProductSku(@Param("productSku") String productSku);
    
    // Find verified compatibility entries
    List<VehicleCompatibilityRegistry> findByIsVerified(Boolean isVerified);
    
    // Find verified products for generation
    @Query("SELECT vcr FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation = :generation AND vcr.isVerified = true")
    List<VehicleCompatibilityRegistry> findVerifiedByGeneration(@Param("generation") BmwGeneration generation);
    
    // Find by series code (through generation relationship)
    @Query("SELECT vcr FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation.series.code = :seriesCode")
    List<VehicleCompatibilityRegistry> findBySeriesCode(@Param("seriesCode") String seriesCode);
    
    // Bulk verification update
    @Modifying
    @Query("UPDATE VehicleCompatibilityRegistry vcr SET vcr.isVerified = :verified " +
           "WHERE vcr.generation = :generation")
    int updateVerificationStatusByGeneration(@Param("generation") BmwGeneration generation,
                                           @Param("verified") Boolean verified);
    
    // Remove compatibility for product (when product is discontinued)
    @Modifying
    @Query("DELETE FROM VehicleCompatibilityRegistry vcr WHERE vcr.productSku = :productSku")
    int removeByProductSku(@Param("productSku") String productSku);
    
    // Count compatible products per generation
    @Query("SELECT COUNT(vcr) FROM VehicleCompatibilityRegistry vcr WHERE vcr.generation = :generation")
    long countByGeneration(@Param("generation") BmwGeneration generation);
    
    // Count verified compatibility entries
    @Query("SELECT COUNT(vcr) FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation = :generation AND vcr.isVerified = true")
    long countVerifiedByGeneration(@Param("generation") BmwGeneration generation);
    
    // Check if product is compatible with generation
    @Query("SELECT CASE WHEN COUNT(vcr) > 0 THEN true ELSE false END " +
           "FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation.code = :generationCode AND vcr.productSku = :productSku")
    boolean isProductCompatible(@Param("generationCode") String generationCode,
                               @Param("productSku") String productSku);
    
    // Find products compatible with multiple generations (bulk operation)
    @Query("SELECT vcr.productSku FROM VehicleCompatibilityRegistry vcr " +
           "WHERE vcr.generation.code IN :generationCodes " +
           "GROUP BY vcr.productSku " +
           "HAVING COUNT(DISTINCT vcr.generation.code) = :generationCount")
    List<String> findProductsCompatibleWithAllGenerations(@Param("generationCodes") List<String> generationCodes,
                                                          @Param("generationCount") long generationCount);
}
