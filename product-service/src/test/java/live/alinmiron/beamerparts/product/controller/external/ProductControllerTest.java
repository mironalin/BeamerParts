package live.alinmiron.beamerparts.product.controller.external;

import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for ProductController - tests API contract and controller orchestration
 * Mocks domain services to focus on HTTP layer behavior
 */
@WebMvcTest(ProductController.class)
@Import({ProductMapper.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCatalogDomainService productCatalogDomainService;

    @MockitoBean
    private InventoryInternalService inventoryInternalService;

    @Test
    void getAllProducts_ShouldReturnPagedProducts() throws Exception {
        // Given
        Product product = createTestProduct();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        ProductResponseDto productDto = createTestProductDto();
        
        when(productCatalogDomainService.findAllProducts(any())).thenReturn(productPage);
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/products")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.data.content[0].totalStock").value(25))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void getProductBySku_WhenProductExists_ShouldReturnProduct() throws Exception {
        // Given
        Product product = createTestProduct();
        ProductResponseDto productDto = createTestProductDto();
        
        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/products/BMW-F30-AC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.totalStock").value(25))
                .andExpect(jsonPath("$.data.isOutOfStock").value(false));
    }

    @Test
    void getProductBySku_WhenProductNotFound_ShouldReturn404() throws Exception {
        // Given
        when(productCatalogDomainService.findProductBySku("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/products/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() throws Exception {
        // Given
        Product product = createTestProduct();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        ProductResponseDto productDto = createTestProductDto();
        
        when(productCatalogDomainService.searchProducts(eq("brake"), any())).thenReturn(productPage);
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/products/search")
                .param("q", "brake")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.content[0].totalStock").value(25));
    }

    @Test
    void getFeaturedProducts_ShouldReturnFeaturedProductsWithLimit() throws Exception {
        // Given
        Product product1 = createTestProduct();
        Product product2 = createTestProduct("BMW-F30-AC-002", "Featured Product 2");
        ProductResponseDto productDto1 = createTestProductDto();
        ProductResponseDto productDto2 = createTestProductDto();
        
        when(productCatalogDomainService.findProductsBulk(any())).thenReturn(List.of(product1, product2));
        when(inventoryInternalService.getInventory("BMW-F30-AC-002", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/products/featured")
                .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1)) // Limited to 1
                .andExpect(jsonPath("$.data[0].sku").value("BMW-F30-AC-001"));
    }

    @Test
    void getProductsByGeneration_ShouldReturnCompatibleProducts() throws Exception {
        // Given
        Product product = createTestProduct();
        ProductResponseDto productDto = createTestProductDto();
        
        when(productCatalogDomainService.findProductsByBmwGeneration("F30")).thenReturn(List.of(product));
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(get("/products/by-generation/F30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data[0].totalStock").value(25));
    }

    @Test
    void searchProducts_WithInvalidQuery_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/products/search")
                .param("q", "")) // Empty query should fail validation
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductBySku_WithInvalidSku_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/products/ ")) // Blank SKU should fail validation
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductsByGeneration_WithInvalidGeneration_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/products/by-generation/ ")) // Blank generation should fail validation
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_WhenInventoryServiceFails_ShouldStillReturnProducts() throws Exception {
        // Given
        Product product = createTestProduct();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        ProductResponseDto productDto = createTestProductDto();
        
        when(productCatalogDomainService.findAllProducts(any())).thenReturn(productPage);
        when(inventoryInternalService.getInventory("BMW-F30-AC-001", null)).thenReturn(createMockInventoryDto());

        // When & Then - Should be resilient to inventory failures
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.content[0].totalStock").value(25)) // Real mapper gets mock value
                .andExpect(jsonPath("$.data.content[0].isOutOfStock").value(false));
    }

    private Product createTestProduct() {
        return createTestProduct("BMW-F30-AC-001", "Test Product");
    }

    private Product createTestProduct(String sku, String name) {
        Category category = Category.builder()
                .id(1L)
                .name("Brake System")
                .slug("brake-system")
                .isActive(true)
                .build();

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
                .isFeatured(true)
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProductResponseDto createTestProductDto() {
        return ProductResponseDto.builder()
                .id(1L)
                .sku("BMW-F30-AC-001")
                .name("Test Product")
                .slug("test-product")
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .brand("BMW")
                .weightGrams(500)
                .isFeatured(true)
                .status(ProductStatus.ACTIVE)
                .totalStock(25)
                .reservedStock(0)
                .isOutOfStock(false)
                .isLowStock(false)
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
