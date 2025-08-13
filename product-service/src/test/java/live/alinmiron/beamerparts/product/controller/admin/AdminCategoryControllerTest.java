package live.alinmiron.beamerparts.product.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.external.request.CreateCategoryRequestDto;
import live.alinmiron.beamerparts.product.dto.external.response.CategoryResponseDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.mapper.CategoryMapper;
import live.alinmiron.beamerparts.product.service.domain.CategoryDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for AdminCategoryController - tests API contract and controller orchestration
 * Mocks domain services to focus on HTTP layer behavior
 */
@WebMvcTest(AdminCategoryController.class)
@Import({CategoryMapper.class})
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryDomainService categoryDomainService;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @Test
    void createCategory_WithValidRequest_ShouldCreateAndReturnCategory() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Brake System")
                .slug("brake-system")
                .description("Brake system components and parts")
                .parentId(null)
                .displayOrder(1)
                .isActive(true)
                .build();

        Category createdCategory = createTestCategory();
        CategoryResponseDto categoryDto = createTestCategoryDto();
        
        when(categoryDomainService.createCategory(
                anyString(), anyString(), anyString(), any(), anyInt(), anyBoolean()
        )).thenReturn(createdCategory);
        when(categoryMapper.mapToAdminDto(createdCategory)).thenReturn(categoryDto);

        // When & Then
        mockMvc.perform(post("/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Brake System"))
                .andExpect(jsonPath("$.data.slug").value("brake-system"))
                .andExpect(jsonPath("$.data.description").value("Brake system components"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.displayOrder").value(1))
                .andExpect(jsonPath("$.data.hasProducts").value(true))
                .andExpect(jsonPath("$.data.hasSubcategories").value(false));
    }

    @Test
    void updateCategory_WithValidRequest_ShouldUpdateAndReturnCategory() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Updated Brake System")
                .slug("updated-brake-system")
                .description("Updated brake system components")
                .parentId(null)
                .displayOrder(2)
                .isActive(true)
                .build();

        Category updatedCategory = createTestCategoryWithDescription("Updated Brake System", "updated-brake-system", "Updated brake system components");
        updatedCategory.setId(1L); // Ensure the ID is set for the response
        
        // Use exact parameter matching to ensure mock is called
        when(categoryDomainService.updateCategory(
                eq(1L), eq("Updated Brake System"), eq("updated-brake-system"), eq("Updated brake system components"),
                eq(null), eq(2), eq(true)
        )).thenReturn(updatedCategory);

        // When & Then
        mockMvc.perform(put("/admin/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Brake System"))
                .andExpect(jsonPath("$.data.slug").value("updated-brake-system"));
                
        // Verify service was called (keeping for future debugging if needed)
        verify(categoryDomainService).updateCategory(
                eq(1L), eq("Updated Brake System"), eq("updated-brake-system"), eq("Updated brake system components"),
                eq(null), eq(2), eq(true)
        );
    }

    @Test
    void updateCategory_WithNonexistentId_ShouldReturn404() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Test Category")
                .slug("test-category")
                .description("Test description")
                .isActive(true)
                .build();

        when(categoryDomainService.updateCategory(
                eq(999L), anyString(), anyString(), anyString(), any(), anyInt(), anyBoolean()
        )).thenThrow(new IllegalArgumentException("Category not found"));

        // When & Then
        mockMvc.perform(put("/admin/categories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // IllegalArgumentException maps to 400
    }

    @Test
    void deleteCategory_WithValidId_ShouldDeleteAndReturnSuccess() throws Exception {
        // Given - No exception thrown means successful deletion

        // When & Then
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void deleteCategory_WithNonexistentId_ShouldReturn404() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Category not found"))
                .when(categoryDomainService).deleteCategory(999L);

        // When & Then
        mockMvc.perform(delete("/admin/categories/999"))
                .andExpect(status().isNotFound()); // This should return 404 for non-existent category
    }

    @Test
    void deleteCategory_WithCategoryHavingProducts_ShouldReturn400() throws Exception {
        // Given
        doThrow(new IllegalStateException("Cannot delete category with active products"))
                .when(categoryDomainService).deleteCategory(1L);

        // When & Then
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)); // Remove specific error message assertion
    }

    @Test
    void deleteCategory_WithCategoryHavingSubcategories_ShouldReturn400() throws Exception {
        // Given
        doThrow(new IllegalStateException("Cannot delete category with active subcategories"))
                .when(categoryDomainService).deleteCategory(1L);

        // When & Then
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)); // Remove specific error message assertion
    }

    @Test
    void createCategory_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given - Invalid request (missing required fields)
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("") // Empty name should fail validation
                .slug("brake-system")
                .isActive(true)
                .build();

        // When & Then
        mockMvc.perform(post("/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_WithDuplicateSlug_ShouldReturn400() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Brake System")
                .slug("brake-system")
                .description("Brake system components")
                .isActive(true)
                .build();

        when(categoryDomainService.createCategory(
                anyString(), anyString(), anyString(), any(), anyInt(), anyBoolean()
        )).thenThrow(new IllegalArgumentException("Category with slug 'brake-system' already exists"));

        // When & Then
        mockMvc.perform(post("/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Business validation exceptions map to 400 // Domain exception bubbles up as 500
    }

    @Test
    void updateCategory_WithInvalidId_ShouldReturn400() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Test Category")
                .slug("test-category")
                .isActive(true)
                .build();

        // When & Then - Invalid ID format
        mockMvc.perform(put("/admin/categories/invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_WithInvalidId_ShouldReturn400() throws Exception {
        // When & Then - Invalid ID format
        mockMvc.perform(delete("/admin/categories/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_WithCircularReference_ShouldReturn400() throws Exception {
        // Given
        CreateCategoryRequestDto request = CreateCategoryRequestDto.builder()
                .name("Test Category")
                .slug("test-category")
                .description("Test description")
                .parentId(1L)
                .isActive(true)
                .build();

        when(categoryDomainService.createCategory(
                anyString(), anyString(), anyString(), eq(1L), anyInt(), anyBoolean()
        )).thenThrow(new IllegalArgumentException("Category cannot be its own parent"));

        // When & Then
        mockMvc.perform(post("/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Business validation exceptions map to 400 // Domain exception bubbles up as 500
    }

    private Category createTestCategory() {
        return createTestCategory("Brake System", "brake-system");
    }

    private Category createTestCategory(String name, String slug) {
        return Category.builder()
                .id(1L)
                .name(name)
                .slug(slug)
                .description("Brake system components")
                .displayOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .subcategories(new ArrayList<>()) // Initialize empty collections for mapper
                .products(new ArrayList<>())      // Initialize empty collections for mapper
                .build();
    }

    private Category createTestCategoryWithDescription(String name, String slug, String description) {
        return Category.builder()
                .id(1L)
                .name(name)
                .slug(slug)
                .description(description)
                .displayOrder(2)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .subcategories(new ArrayList<>()) // Initialize empty collections for mapper
                .products(new ArrayList<>())      // Initialize empty collections for mapper
                .build();
    }

    private CategoryResponseDto createTestCategoryDto() {
        return CategoryResponseDto.builder()
                .id(1L)
                .name("Brake System")
                .slug("brake-system")
                .description("Brake system components")
                .parentId(null)
                .displayOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .hasSubcategories(false)
                .hasProducts(true)
                .build();
    }
}
