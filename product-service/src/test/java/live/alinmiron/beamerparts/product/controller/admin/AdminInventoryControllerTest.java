package live.alinmiron.beamerparts.product.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.external.request.UpdateStockRequestDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.dto.external.response.InventoryResponseDto;
import live.alinmiron.beamerparts.product.entity.Inventory;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.mapper.InventoryMapper;
import live.alinmiron.beamerparts.product.service.domain.InventoryDomainService;
import live.alinmiron.beamerparts.product.service.domain.ProductCatalogDomainService;
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
 * @WebMvcTest for AdminInventoryController - tests API contract and controller orchestration
 * Mocks domain services to focus on HTTP layer behavior
 */
@WebMvcTest(AdminInventoryController.class)
@Import({InventoryMapper.class})
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryDomainService inventoryDomainService;

    @MockitoBean
    private ProductCatalogDomainService productCatalogDomainService;



    @Test
    void updateStock_WithValidRequest_ShouldUpdateAndReturnInventory() throws Exception {
        // Given
        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 50);
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.ADD)
                .quantity(25)
                .reason("Restocking from supplier")
                .build();

        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryDomainService.getAvailableQuantity(eq(product), isNull())).thenReturn(50);
        when(inventoryDomainService.getInventory(eq(product), isNull())).thenReturn(Optional.of(inventory));


        // When & Then
        mockMvc.perform(put("/admin/inventory/BMW-F30-AC-001/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.quantityAvailable").value(50))
                .andExpect(jsonPath("$.data.quantityReserved").value(10))
                .andExpect(jsonPath("$.data.totalQuantity").value(60))
                .andExpect(jsonPath("$.data.isLowStock").value(false))
                .andExpect(jsonPath("$.data.isOutOfStock").value(false));
    }

    @Test
    void updateStock_WithRemoveAction_ShouldReduceStock() throws Exception {
        // Given
        Product product = createTestProduct();
        Inventory inventory = createTestInventoryLowStock(product, 25); // Will be low stock after removal
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.REMOVE)
                .quantity(25)
                .reason("Damaged goods removal")
                .build();

        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryDomainService.getAvailableQuantity(eq(product), isNull())).thenReturn(50); // Current stock
        when(inventoryDomainService.getInventory(eq(product), isNull())).thenReturn(Optional.of(inventory));

        // When & Then
        mockMvc.perform(put("/admin/inventory/BMW-F30-AC-001/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantityAvailable").value(25))
                .andExpect(jsonPath("$.data.isLowStock").value(true)); // Should be low stock now
    }

    @Test
    void updateStock_WithSetAction_ShouldSetExactQuantity() throws Exception {
        // Given
        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100); // Set to exact amount
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.SET)
                .quantity(100)
                .reason("Annual inventory count")
                .build();

        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryDomainService.getAvailableQuantity(eq(product), isNull())).thenReturn(50); // Current stock
        when(inventoryDomainService.getInventory(eq(product), isNull())).thenReturn(Optional.of(inventory));

        // When & Then
        mockMvc.perform(put("/admin/inventory/BMW-F30-AC-001/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantityAvailable").value(100));
    }

    @Test
    void updateStock_WithNonexistentProduct_ShouldReturn404() throws Exception {
        // Given
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.ADD)
                .quantity(25)
                .reason("Test")
                .build();

        when(productCatalogDomainService.findProductBySku("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/admin/inventory/NONEXISTENT/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventory_WithValidSku_ShouldReturnInventoryDetails() throws Exception {
        // Given
        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 75);

        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryDomainService.getInventory(eq(product), isNull())).thenReturn(Optional.of(inventory));

        // When & Then
        mockMvc.perform(get("/admin/inventory/BMW-F30-AC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.quantityAvailable").value(75))
                .andExpect(jsonPath("$.data.quantityReserved").value(10))
                .andExpect(jsonPath("$.data.minimumStockLevel").value(5))
                .andExpect(jsonPath("$.data.reorderPoint").value(10))
                .andExpect(jsonPath("$.data.displayName").value("Test Product"));
    }

    @Test
    void getInventory_WithNonexistentProduct_ShouldReturn404() throws Exception {
        // Given
        when(productCatalogDomainService.findProductBySku("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/inventory/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventory_WithNoInventoryRecord_ShouldReturn404() throws Exception {
        // Given
        Product product = createTestProduct();
        when(productCatalogDomainService.findProductBySku("BMW-F30-AC-001")).thenReturn(Optional.of(product));
        when(inventoryDomainService.getInventory(eq(product), isNull())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/inventory/BMW-F30-AC-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStock_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given - Invalid request (missing required fields)
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(null) // Missing required action
                .quantity(25)
                .build();

        // When & Then
        mockMvc.perform(put("/admin/inventory/BMW-F30-AC-001/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStock_WithNegativeQuantity_ShouldReturn400() throws Exception {
        // Given - Invalid quantity
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.ADD)
                .quantity(-5) // Invalid negative quantity
                .reason("Test")
                .build();

        // When & Then
        mockMvc.perform(put("/admin/inventory/BMW-F30-AC-001/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStock_WithBlankSku_ShouldReturn400() throws Exception {
        // Given
        UpdateStockRequestDto request = UpdateStockRequestDto.builder()
                .action(UpdateStockRequestDto.StockAction.ADD)
                .quantity(25)
                .reason("Test")
                .build();

        // When & Then
        mockMvc.perform(put("/admin/inventory/ /stock") // Blank SKU
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Product createTestProduct() {
        Category category = Category.builder()
                .id(1L)
                .name("Brake System")
                .slug("brake-system")
                .isActive(true)
                .build();

        return Product.builder()
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
    }

    private Inventory createTestInventory(Product product, int availableQuantity) {
        return Inventory.builder()
                .id(1L)
                .product(product)
                .quantityAvailable(availableQuantity)
                .quantityReserved(10)
                .minimumStockLevel(5)
                .reorderPoint(10)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private Inventory createTestInventoryLowStock(Product product, int availableQuantity) {
        return Inventory.builder()
                .id(1L)
                .product(product)
                .quantityAvailable(availableQuantity)
                .quantityReserved(10)
                .minimumStockLevel(5)
                .reorderPoint(30) // Higher reorder point to trigger low stock
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
