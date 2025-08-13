package live.alinmiron.beamerparts.product.controller.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.internal.request.BulkProductRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.ProductValidationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductValidationDto;
import live.alinmiron.beamerparts.product.service.internal.ProductInternalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductInternalController.class)
class ProductInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductInternalService productInternalService;

    @Test
    void getProductBySku_ShouldReturnProduct_WhenProductExists() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        ProductInternalDto product = ProductInternalDto.builder()
                .sku(sku)
                .name("Test BMW Part")
                .basePrice(BigDecimal.valueOf(99.99))
                .status("ACTIVE")
                .build();
        
        when(productInternalService.getProductBySku(eq(sku), eq(false), eq(false), eq(false)))
                .thenReturn(product);

        // When & Then
        mockMvc.perform(get("/internal/products/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value(sku))
                .andExpect(jsonPath("$.data.name").value("Test BMW Part"))
                .andExpect(jsonPath("$.data.basePrice").value(99.99));
    }

    @Test
    void getProductBySku_WithFlags_ShouldPassCorrectParameters() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        ProductInternalDto product = ProductInternalDto.builder()
                .sku(sku)
                .name("Test BMW Part")
                .basePrice(BigDecimal.valueOf(99.99))
                .status("ACTIVE")
                .build();

        when(productInternalService.getProductBySku(eq(sku), eq(true), eq(true), eq(true)))
                .thenReturn(product);

        // When & Then
        mockMvc.perform(get("/internal/products/{sku}", sku)
                        .param("includeInventory", "true")
                        .param("includeVariants", "true")
                        .param("includeCompatibility", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value(sku));
    }

    @Test
    void getProductsBulk_ShouldReturnProducts_WhenValidRequest() throws Exception {
        // Given
        BulkProductRequestDto request = BulkProductRequestDto.builder()
                .skus(Arrays.asList("BMW-TEST-001", "BMW-TEST-002"))
                .includeInventory(true)
                .build();

        List<ProductInternalDto> products = Arrays.asList(
                ProductInternalDto.builder()
                        .sku("BMW-TEST-001")
                        .name("Test BMW Part 1")
                        .basePrice(BigDecimal.valueOf(99.99))
                        .status("ACTIVE")
                        .build(),
                ProductInternalDto.builder()
                        .sku("BMW-TEST-002")
                        .name("Test BMW Part 2")
                        .basePrice(BigDecimal.valueOf(149.99))
                        .status("ACTIVE")
                        .build()
        );

        when(productInternalService.getProductsBulk(any(BulkProductRequestDto.class)))
                .thenReturn(products);

        // When & Then
        mockMvc.perform(post("/internal/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sku").value("BMW-TEST-001"))
                .andExpect(jsonPath("$.data[1].sku").value("BMW-TEST-002"));
    }

    @Test
    void getProductsBulk_ShouldReturnBadRequest_WhenEmptySkuList() throws Exception {
        // Given
        BulkProductRequestDto request = BulkProductRequestDto.builder()
                .skus(Collections.emptyList())
                .build();

        // When & Then
        mockMvc.perform(post("/internal/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateProducts_ShouldReturnValidationResults_WhenValidRequest() throws Exception {
        // Given
        List<ProductValidationRequestDto> request = Arrays.asList(
                ProductValidationRequestDto.builder()
                        .sku("BMW-TEST-001")
                        .requestedQuantity(2)
                        .build()
        );

        List<ProductValidationDto> validationResults = Arrays.asList(
                ProductValidationDto.builder()
                        .sku("BMW-TEST-001")
                        .exists(true)
                        .isActive(true)
                        .isAvailable(true)
                        .name("Test BMW Part")
                        .currentPrice(BigDecimal.valueOf(99.99))
                        .build()
        );

        when(productInternalService.validateProducts(anyList()))
                .thenReturn(validationResults);

        // When & Then
        mockMvc.perform(post("/internal/products/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("BMW-TEST-001"))
                .andExpect(jsonPath("$.data[0].exists").value(true));
    }

    @Test
    void getProductsByGeneration_ShouldReturnProducts_WhenValidGenerationCode() throws Exception {
        // Given
        String generationCode = "E90";
        List<ProductInternalDto> products = Arrays.asList(
                ProductInternalDto.builder()
                        .sku("BMW-E90-001")
                        .name("E90 Compatible Part")
                        .basePrice(BigDecimal.valueOf(199.99))
                        .status("ACTIVE")
                        .build()
        );

        when(productInternalService.getProductsByGeneration(eq(generationCode), eq(false)))
                .thenReturn(products);

        // When & Then
        mockMvc.perform(get("/internal/products/by-generation/{generationCode}", generationCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("BMW-E90-001"));
    }

    @Test
    void getProductsByGeneration_WithInventory_ShouldPassCorrectFlag() throws Exception {
        // Given
        String generationCode = "E90";
        List<ProductInternalDto> products = Arrays.asList(
                ProductInternalDto.builder()
                        .sku("BMW-E90-001")
                        .name("E90 Compatible Part")
                        .basePrice(BigDecimal.valueOf(199.99))
                        .status("ACTIVE")
                        .build()
        );

        when(productInternalService.getProductsByGeneration(eq(generationCode), eq(true)))
                .thenReturn(products);

        // When & Then
        mockMvc.perform(get("/internal/products/by-generation/{generationCode}", generationCode)
                        .param("includeInventory", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void productExists_ShouldReturnTrue_WhenProductExists() throws Exception {
        // Given
        String sku = "BMW-TEST-001";
        when(productInternalService.productExistsAndActive(eq(sku)))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/internal/products/{sku}/exists", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void productExists_ShouldReturnFalse_WhenProductDoesNotExist() throws Exception {
        // Given
        String sku = "NON-EXISTENT-SKU";
        when(productInternalService.productExistsAndActive(eq(sku)))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/internal/products/{sku}/exists", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }
}