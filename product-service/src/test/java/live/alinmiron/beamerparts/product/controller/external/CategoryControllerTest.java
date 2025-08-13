package live.alinmiron.beamerparts.product.controller.external;

import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.mapper.CategoryMapper;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
import live.alinmiron.beamerparts.product.service.domain.CategoryDomainService;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for CategoryController - tests API contract and controller orchestration
 * Mocks domain services to focus on HTTP layer behavior
 */
@WebMvcTest(CategoryController.class)
@Import({CategoryMapper.class, ProductMapper.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryDomainService categoryDomainService;

    @MockitoBean
    private ProductCatalogDomainService productCatalogDomainService;

    @MockitoBean
    private InventoryInternalService inventoryInternalService;

    @Test
    void getAllCategories_ShouldReturnRootCategories() throws Exception {
        // Given
        Category category = createTestCategoryWithProducts();
        
        when(categoryDomainService.findRootCategories()).thenReturn(List.of(category));

        // When & Then
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Brake System"))
                .andExpect(jsonPath("$.data[0].slug").value("brake-system"))
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                .andExpect(jsonPath("$.data[0].hasProducts").value(true))
                .andExpect(jsonPath("$.data[0].hasSubcategories").value(false));
    }

    @Test
    void getCategoryById_WhenCategoryExists_ShouldReturnCategory() throws Exception {
        // Given
        Category category = createTestCategory();
        
        when(categoryDomainService.findCategoryById(1L)).thenReturn(Optional.of(category));

        // When & Then
        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Brake System"))
                .andExpect(jsonPath("$.data.slug").value("brake-system"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void getCategoryById_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        // Given
        when(categoryDomainService.findCategoryById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsByCategory_ShouldReturnProductsInCategory() throws Exception {
        // Given
        Product product = createTestProduct();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        
        when(productCatalogDomainService.findProductsByCategory(eq(1L), any())).thenReturn(productPage);
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/categories/1/products")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void getProductsByCategory_WhenCategoryHasNoProducts_ShouldReturnEmptyPage() throws Exception {
        // Given
        Page<Product> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productCatalogDomainService.findProductsByCategory(eq(1L), any())).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/categories/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getCategoryById_WithInvalidId_ShouldReturn400() throws Exception {
        // When & Then - Invalid ID format
        mockMvc.perform(get("/categories/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCategories_WhenNoCategoriesExist_ShouldReturnEmptyArray() throws Exception {
        // Given
        when(categoryDomainService.findRootCategories()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getProductsByCategory_WithPagination_ShouldRespectPaginationParameters() throws Exception {
        // Given
        Product product1 = createTestProduct("BMW-F30-AC-001", "Product 1");
        Product product2 = createTestProduct("BMW-F30-AC-002", "Product 2");
        Page<Product> productPage = new PageImpl<>(List.of(product1, product2), PageRequest.of(1, 1), 2);
        
        when(productCatalogDomainService.findProductsByCategory(eq(1L), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/categories/1/products")
                .param("page", "1")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.number").value(1)) // Current page
                .andExpect(jsonPath("$.data.size").value(1));  // Page size
    }

    private Category createTestCategory() {
        return Category.builder()
                .id(1L)
                .name("Brake System")
                .slug("brake-system")
                .description("Brake system components")
                .displayOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Category createTestCategoryWithProducts() {
        Category category = Category.builder()
                .id(1L)
                .name("Brake System")
                .slug("brake-system")
                .description("Brake system components")
                .displayOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .products(new ArrayList<>()) // Initialize the products list
                .build();
        
        // Create a product that belongs to this category
        Product product = Product.builder()
                .id(1L)
                .sku("BMW-F30-AC-001")
                .name("Test Product")
                .slug("test-product")
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("99.99"))
                .category(category)
                .brand("BMW")
                .weightGrams(500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Add the product to the category's products list
        category.getProducts().add(product);
        
        return category;
    }

    private Product createTestProduct() {
        return createTestProduct("BMW-F30-AC-001", "Test Product");
    }

    private Product createTestProduct(String sku, String name) {
        Category category = createTestCategory();

        return Product.builder()
                .id(1L)
                .sku(sku)
                .name(name)
                .slug("test-product")
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("99.99"))
                .category(category)
                .brand("BMW")
                .weightGrams(500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private InventoryInternalDto createMockInventoryDto() {
        return InventoryInternalDto.builder()
                .quantityAvailable(25)
                .quantityReserved(0)
                .isInStock(true)
                .isLowStock(false)
                .build();
    }
}
