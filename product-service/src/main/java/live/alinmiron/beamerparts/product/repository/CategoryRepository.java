package live.alinmiron.beamerparts.product.repository;

import live.alinmiron.beamerparts.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Category repository for database operations
 * Leverages the database indexes for optimal performance
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find by slug (uses unique index)
    Optional<Category> findBySlug(String slug);
    
    // Check if slug exists
    boolean existsBySlug(String slug);
    
    // Find active categories
    List<Category> findByIsActive(Boolean isActive);
    
    // Find root categories (no parent)
    List<Category> findByParentIsNullAndIsActiveOrderByDisplayOrder(Boolean isActive);
    
    // Find subcategories of a parent
    List<Category> findByParentAndIsActiveOrderByDisplayOrder(Category parent, Boolean isActive);
    
    // Find by parent ID
    List<Category> findByParentIdAndIsActiveOrderByDisplayOrder(Long parentId, Boolean isActive);
    
    // Find categories with products
    @Query("SELECT DISTINCT c FROM Category c " +
           "JOIN c.products p WHERE c.isActive = true AND p.status = 'ACTIVE' " +
           "ORDER BY c.displayOrder")
    List<Category> findCategoriesWithActiveProducts();
    
    // Find by name (case-insensitive)
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Count active categories
    long countByIsActive(Boolean isActive);
    
    // Count subcategories
    long countByParentAndIsActive(Category parent, Boolean isActive);
    
    // Check if slug exists for different entity (for updates)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Category c WHERE c.slug = :slug AND c.id != :id")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);
    
    // Find categories by codes (bulk operation)
    @Query("SELECT c FROM Category c WHERE c.slug IN :slugs")
    List<Category> findBySlugs(@Param("slugs") List<String> slugs);
    
    // Find category hierarchy (all ancestors)
    @Query(value = "WITH RECURSIVE category_hierarchy AS (" +
           "  SELECT id, name, slug, parent_id, 0 as level " +
           "  FROM categories WHERE id = :categoryId " +
           "  UNION ALL " +
           "  SELECT c.id, c.name, c.slug, c.parent_id, ch.level + 1 " +
           "  FROM categories c " +
           "  JOIN category_hierarchy ch ON c.id = ch.parent_id" +
           ") SELECT * FROM category_hierarchy ORDER BY level DESC", 
           nativeQuery = true)
    List<Object[]> findCategoryHierarchy(@Param("categoryId") Long categoryId);
}
