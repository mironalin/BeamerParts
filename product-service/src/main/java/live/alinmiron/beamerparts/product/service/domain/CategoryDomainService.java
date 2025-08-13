package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service that encapsulates all category hierarchy business logic.
 * 
 * Business Rules:
 * - Category slugs must be unique across all categories
 * - Categories can only be created under active parent categories
 * - Circular parent-child references are prevented
 * - Categories with active products or subcategories cannot be deleted
 * - Category hierarchy must maintain referential integrity
 * - Category tree building preserves display order and active status
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryDomainService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new category with business validation
     * 
     * Business Rules:
     * - Slug must be unique
     * - Parent must exist and be active (if specified)
     * - Cannot create under inactive parent
     */
    @Transactional
    public Category createCategory(String name, String slug, String description, 
                                  Long parentId, Integer displayOrder, Boolean isActive) {
        log.debug("Creating new category with slug: {}", slug);
        
        // Business Rule: Validate slug uniqueness
        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Category with slug '" + slug + "' already exists");
        }
        
        // Business Rule: Validate parent exists and is active if specified
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found with ID: " + parentId));
            
            if (!parent.getIsActive()) {
                throw new IllegalArgumentException("Cannot create category under inactive parent");
            }
        }
        
        // Create category entity
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .parent(parent)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .build();
        
        category = categoryRepository.save(category);
        log.info("Created new category: {} with ID: {}", category.getName(), category.getId());
        
        return category;
    }

    /**
     * Update category with business validation
     * 
     * Business Rules:
     * - Slug must remain unique if changed
     * - Cannot set self as parent (circular reference)
     * - New parent must exist and be active
     * - Cannot move to inactive parent
     */
    @Transactional
    public Category updateCategory(Long id, String name, String slug, String description,
                                  Long parentId, Integer displayOrder, Boolean isActive) {
        log.debug("Updating category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        
        // Business Rule: Validate slug uniqueness if changed
        if (!category.getSlug().equals(slug) && 
            categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("Category with slug '" + slug + "' already exists");
        }
        
        // Business Rule: Validate parent changes
        if (parentId != null && 
            !parentId.equals(category.getParent() != null ? category.getParent().getId() : null)) {
            
            // Business Rule: Prevent circular references
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            
            Category newParent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found with ID: " + parentId));
            
            // Business Rule: Cannot move to inactive parent
            if (!newParent.getIsActive()) {
                throw new IllegalArgumentException("Cannot move category under inactive parent");
            }
            
            category.setParent(newParent);
        } else if (parentId == null) {
            category.setParent(null);
        }
        
        // Update fields
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setDisplayOrder(displayOrder);
        category.setIsActive(isActive);
        
        category = categoryRepository.save(category);
        log.info("Updated category: {} with ID: {}", category.getName(), category.getId());
        
        return category;
    }

    /**
     * Delete category with business validation
     * 
     * Business Rules:
     * - Cannot delete category with active products
     * - Cannot delete category with active subcategories
     * - Deletion is soft delete (mark as inactive)
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.debug("Deleting category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        
        // Business Rule: Check if category has active products
        long activeProductCount = productRepository.countByCategoryAndStatus(category, ProductStatus.ACTIVE);
        if (activeProductCount > 0) {
            throw new IllegalStateException("Cannot delete category with active products");
        }
        
        // Business Rule: Check if category has active subcategories
        long activeSubcategoryCount = categoryRepository.countByParentAndIsActive(category, true);
        if (activeSubcategoryCount > 0) {
            throw new IllegalStateException("Cannot delete category with active subcategories");
        }
        
        category.setIsActive(false);
        categoryRepository.save(category);
        
        log.info("Deleted (deactivated) category: {} with ID: {}", category.getName(), category.getId());
    }

    /**
     * Find category by ID
     */
    public Optional<Category> findCategoryById(Long id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    /**
     * Find category by slug
     */
    public Optional<Category> findCategoryBySlug(String slug) {
        log.debug("Finding category by slug: {}", slug);
        return categoryRepository.findBySlug(slug);
    }

    /**
     * Get all root categories (no parent) ordered by display order
     */
    public List<Category> findRootCategories() {
        log.debug("Finding all root categories");
        return categoryRepository.findByParentIsNullAndIsActiveOrderByDisplayOrder(true);
    }

    /**
     * Get subcategories of a parent category
     */
    public List<Category> findSubcategories(Long parentId) {
        log.debug("Finding subcategories for parent ID: {}", parentId);
        return categoryRepository.findByParentIdAndIsActiveOrderByDisplayOrder(parentId, true);
    }

    /**
     * Get categories that have active products
     */
    public List<Category> findCategoriesWithProducts() {
        log.debug("Finding categories with active products");
        return categoryRepository.findCategoriesWithActiveProducts();
    }

    /**
     * Search categories by name (case-insensitive)
     */
    public List<Category> searchCategoriesByName(String searchTerm) {
        log.debug("Searching categories with term: {}", searchTerm);
        return categoryRepository.findByNameContainingIgnoreCase(searchTerm);
    }

    /**
     * Get category statistics
     */
    public CategoryStats getCategoryStatistics() {
        long totalCategories = categoryRepository.countByIsActive(true);
        long rootCategories = categoryRepository.findByParentIsNullAndIsActiveOrderByDisplayOrder(true).size();
        
        return new CategoryStats(totalCategories, rootCategories);
    }

    /**
     * Check if category exists and is active
     */
    public boolean isCategoryActiveAndExists(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        
        return categoryRepository.findById(categoryId)
                .map(Category::getIsActive)
                .orElse(false);
    }

    /**
     * Validate category for product assignment
     * 
     * Business Rule: Products can only be assigned to active categories
     */
    public void validateCategoryForProductAssignment(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        if (!category.getIsActive()) {
            throw new IllegalArgumentException("Cannot assign product to inactive category");
        }
    }

    /**
     * Value object for category statistics
     */
    public static class CategoryStats {
        private final Long totalActiveCategories;
        private final Long rootCategories;
        
        public CategoryStats(Long totalActiveCategories, Long rootCategories) {
            this.totalActiveCategories = totalActiveCategories;
            this.rootCategories = rootCategories;
        }
        
        public Long getTotalActiveCategories() {
            return totalActiveCategories;
        }
        
        public Long getRootCategories() {
            return rootCategories;
        }
    }
}
