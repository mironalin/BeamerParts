package live.alinmiron.beamerparts.product.controller.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.internal.request.BulkStockCheckRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReservationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.StockReleaseRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.InventoryInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.StockReservationDto;
import live.alinmiron.beamerparts.product.service.internal.InventoryInternalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryInternalController.class)
class InventoryInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryInternalService inventoryInternalService;

    @Test
    void reserveStock_ShouldReturnReservation_WhenSuccessful() throws Exception {
        // Given
        StockReservationRequestDto request = StockReservationRequestDto.builder()
                .productSku("BMW-TEST-001")
                .quantity(2)
                .userId("user123")
                .source("cart")
                .build();

        StockReservationDto reservation = StockReservationDto.success(
                "res-123", "BMW-TEST-001", null, 2, "user123", 8, 
                LocalDateTime.now().plusMinutes(30)
        );

        when(inventoryInternalService.reserveStock(any(StockReservationRequestDto.class)))
                .thenReturn(reservation);

        // When & Then
        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationId").value("res-123"))
                .andExpect(jsonPath("$.data.productSku").value("BMW-TEST-001"))
                .andExpect(jsonPath("$.data.quantityReserved").value(2))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void reserveStock_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given - missing required fields
        StockReservationRequestDto request = StockReservationRequestDto.builder()
                .quantity(2)
                .build();

        // When & Then
        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void releaseStock_ShouldReturnSuccess_WhenValid() throws Exception {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .reservationId("res-123")
                .userId("user123")
                .reason("cancelled")
                .build();

        // When & Then
        mockMvc.perform(post("/internal/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stock released successfully"));
    }

    @Test
    void releaseStock_ShouldReturnBadRequest_WhenMissingReservationId() throws Exception {
        // Given
        StockReleaseRequestDto request = StockReleaseRequestDto.builder()
                .userId("user123")
                .reason("cancelled")
                .build();

        // When & Then
        mockMvc.perform(post("/internal/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkStockCheck_ShouldReturnInventoryList_WhenValidRequest() throws Exception {
        // Given
        BulkStockCheckRequestDto request = BulkStockCheckRequestDto.builder()
                .items(Arrays.asList(
                        BulkStockCheckRequestDto.StockCheckItemDto.builder()
                                .sku("BMW-TEST-001")
                                .requestedQuantity(2)
                                .build(),
                        BulkStockCheckRequestDto.StockCheckItemDto.builder()
                                .sku("BMW-TEST-002")
                                .requestedQuantity(1)
                                .build()
                ))
                .source("cart")
                .build();

        List<InventoryInternalDto> inventoryList = Arrays.asList(
                InventoryInternalDto.builder()
                        .productSku("BMW-TEST-001")
                        .quantityAvailable(10)
                        .isInStock(true)
                        .build(),
                InventoryInternalDto.builder()
                        .productSku("BMW-TEST-002")
                        .quantityAvailable(5)
                        .isInStock(true)
                        .build()
        );

        when(inventoryInternalService.bulkStockCheck(any(BulkStockCheckRequestDto.class)))
                .thenReturn(inventoryList);

        // When & Then
        mockMvc.perform(post("/internal/inventory/bulk-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productSku").value("BMW-TEST-001"))
                .andExpect(jsonPath("$.data[0].quantityAvailable").value(10))
                .andExpect(jsonPath("$.data[1].productSku").value("BMW-TEST-002"));
    }

    @Test
    void bulkStockCheck_ShouldReturnBadRequest_WhenEmptyItems() throws Exception {
        // Given
        BulkStockCheckRequestDto request = BulkStockCheckRequestDto.builder()
                .items(Collections.emptyList())
                .build();

        // When & Then
        mockMvc.perform(post("/internal/inventory/bulk-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInventory_ShouldReturnInventory_WhenProductExists() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        InventoryInternalDto inventory = InventoryInternalDto.builder()
                .productSku(sku)
                .quantityAvailable(15)
                .quantityReserved(5)
                .isInStock(true)
                .isLowStock(false)
                .build();

        when(inventoryInternalService.getInventory(eq(sku), isNull()))
                .thenReturn(inventory);

        // When & Then
        mockMvc.perform(get("/internal/inventory/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productSku").value(sku))
                .andExpect(jsonPath("$.data.quantityAvailable").value(15))
                .andExpect(jsonPath("$.data.inStock").value(true));
    }

    @Test
    void getInventory_WithVariant_ShouldPassVariantSku() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        String variantSku = "SIZE-L";
        InventoryInternalDto inventory = InventoryInternalDto.builder()
                .productSku(sku)
                .variantSkuSuffix(variantSku)
                .quantityAvailable(8)
                .isInStock(true)
                .build();

        when(inventoryInternalService.getInventory(eq(sku), eq(variantSku)))
                .thenReturn(inventory);

        // When & Then
        mockMvc.perform(get("/internal/inventory/{sku}", sku)
                        .param("variantSku", variantSku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productSku").value(sku))
                .andExpect(jsonPath("$.data.variantSkuSuffix").value(variantSku));
    }

    @Test
    void updateStock_ShouldReturnUpdatedInventory_WhenValid() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        InventoryInternalDto updatedInventory = InventoryInternalDto.builder()
                .productSku(sku)
                .quantityAvailable(25)
                .isInStock(true)
                .build();

        when(inventoryInternalService.updateStock(eq(sku), isNull(), eq(25), eq("restock"), eq("admin123")))
                .thenReturn(updatedInventory);

        // When & Then
        mockMvc.perform(put("/internal/inventory/{sku}/stock", sku)
                        .param("quantity", "25")
                        .param("reason", "restock")
                        .param("userCode", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productSku").value(sku))
                .andExpect(jsonPath("$.data.quantityAvailable").value(25));
    }

    @Test
    void isStockAvailable_ShouldReturnTrue_WhenSufficientStock() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        when(inventoryInternalService.isStockAvailable(eq(sku), isNull(), eq(3)))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/internal/inventory/{sku}/available", sku)
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void isStockAvailable_ShouldReturnFalse_WhenInsufficientStock() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        when(inventoryInternalService.isStockAvailable(eq(sku), isNull(), eq(100)))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/internal/inventory/{sku}/available", sku)
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void isStockAvailable_WithVariant_ShouldPassVariantSku() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        String variantSku = "COLOR-RED";
        when(inventoryInternalService.isStockAvailable(eq(sku), eq(variantSku), eq(2)))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/internal/inventory/{sku}/available", sku)
                        .param("variantSku", variantSku)
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
