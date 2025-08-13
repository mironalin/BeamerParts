package live.alinmiron.beamerparts.product.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.external.request.CreateProductRequestDto;
import live.alinmiron.beamerparts.product.dto.external.response.ProductResponseDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.mapper.ProductMapper;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for AdminProductController - tests API contract and controller orchestration
 * Mocks domain services to focus on HTTP layer behavior
 */
@WebMvcTest(AdminProductController.class)
@Import({ProductMapper.class})
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductCatalogDomainService productCatalogDomainService;

    @MockitoBean
    private InventoryInternalService inventoryInternalService;

    @Test
    void createProduct_WithValidRequest_ShouldCreateAndReturnProduct() throws Exception {
        // Given
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("BMW F30 Brake Pad")
                .slug("bmw-f30-brake-pad")
                .sku("BMW-F30-BP-001")
                .description("High performance brake pad for BMW F30")
                .shortDescription("BMW F30 brake pad")
                .basePrice(new BigDecimal("89.99"))
                .categoryId(1L)
                .brand("BMW")
                .weightGrams(800)
                .isFeatured(true)
                .status(ProductStatus.ACTIVE)
                .build();

        Product createdProduct = createTestProduct();
        
        when(productCatalogDomainService.createProduct(
                eq(request.getName()), eq(request.getSlug()), eq(request.getSku()),
                eq(request.getDescription()), eq(request.getShortDescription()),
                eq(request.getBasePrice()), eq(request.getCategoryId()), eq(request.getBrand()),
                eq(request.getWeightGrams()), eq(request.getDimensionsJson()),
                eq(request.getIsFeatured()), eq(request.getStatus())
        )).thenReturn(createdProduct);
        when(inventoryInternalService.getInventory(createdProduct.getSku(), null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(post("/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sku").value("BMW-F30-AC-001"))
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.brand").value("BMW"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.isFeatured").value(true));
    }

    @Test
    void updateProduct_WithValidRequest_ShouldUpdateAndReturnProduct() throws Exception {
        // Given
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("Updated BMW F30 Brake Pad")
                .slug("updated-bmw-f30-brake-pad")
                .sku("BMW-F30-BP-001-UPDATED")
                .description("Updated high performance brake pad")
                .shortDescription("Updated BMW F30 brake pad")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .brand("BMW")
                .weightGrams(850)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .build();

        Product existingProduct = createTestProduct();
        Product updatedProduct = createTestProduct("BMW-F30-BP-001-UPDATED", "Updated BMW F30 Brake Pad");

        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(existingProduct));
        when(productCatalogDomainService.updateProduct(
                eq(existingProduct.getId()), eq(request.getName()), eq(request.getSlug()),
                eq(request.getSku()), eq(request.getDescription()), eq(request.getShortDescription()),
                eq(request.getBasePrice()), eq(request.getCategoryId()), eq(request.getBrand()),
                eq(request.getWeightGrams()), eq(request.getDimensionsJson()),
                eq(request.getIsFeatured()), eq(request.getStatus())
        )).thenReturn(updatedProduct);
        when(inventoryInternalService.getInventory(updatedProduct.getSku(), null)).thenReturn(createMockInventoryDto());

        // When & Then
        mockMvc.perform(put("/admin/products/BMW-F30-AC-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("BMW-F30-BP-001-UPDATED"))
                .andExpect(jsonPath("$.data.name").value("Updated BMW F30 Brake Pad"));
    }

    @Test
    void updateProduct_WithNonexistentSku_ShouldReturn404() throws Exception {
        // Given
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("NONEXISTENT")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .brand("BMW")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productCatalogDomainService.findProductBySku("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/admin/products/NONEXISTENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_WithValidSku_ShouldDeleteAndReturnSuccess() throws Exception {
        // Given
        when(productCatalogDomainService.deleteProductBySku("BMW-F30-AC-001")).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/admin/products/BMW-F30-AC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void deleteProduct_WithNonexistentSku_ShouldReturn404() throws Exception {
        // Given
        when(productCatalogDomainService.deleteProductBySku("NONEXISTENT")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/admin/products/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given - Invalid request (missing required fields)
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("") // Empty name should fail validation
                .sku("BMW-F30-AC-001")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .build();

        // When & Then
        mockMvc.perform(post("/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_WithNegativePrice_ShouldReturn400() throws Exception {
        // Given - Invalid price
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("BMW-F30-AC-001")
                .basePrice(new BigDecimal("-10.00")) // Negative price
                .categoryId(1L)
                .brand("BMW")
                .status(ProductStatus.ACTIVE)
                .build();

        // When & Then
        mockMvc.perform(post("/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_WithBlankSku_ShouldReturn400() throws Exception {
        // Given
        CreateProductRequestDto request = CreateProductRequestDto.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("BMW-F30-AC-001")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .brand("BMW")
                .status(ProductStatus.ACTIVE)
                .build();

        // When & Then
        mockMvc.perform(put("/admin/products/ ") // Blank SKU
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProduct_WithBlankSku_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(delete("/admin/products/ ")) // Blank SKU
                .andExpect(status().isBadRequest());
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

    private InventoryInternalDto createMockInventoryDto() {
        return InventoryInternalDto.builder()
                .quantityAvailable(25)
                .quantityReserved(0)
                .isInStock(true)
                .isLowStock(false)
                .build();
    }
}
