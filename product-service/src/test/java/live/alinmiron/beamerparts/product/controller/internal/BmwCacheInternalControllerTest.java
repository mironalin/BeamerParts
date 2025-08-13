package live.alinmiron.beamerparts.product.controller.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwCacheBulkSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwSeriesSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwGenerationSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwSeriesCacheDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwGenerationCacheDto;
import live.alinmiron.beamerparts.product.service.internal.BmwCacheInternalService;
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

@WebMvcTest(BmwCacheInternalController.class)
class BmwCacheInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BmwCacheInternalService bmwCacheInternalService;

    @Test
    void syncSeries_ShouldReturnUpdatedSeries_WhenValidRequest() throws Exception {
        // Given
        BmwSeriesSyncRequestDto request = BmwSeriesSyncRequestDto.builder()
                .code("3")
                .name("Seria 3")
                .displayOrder(1)
                .isActive(true)
                .build();

        BmwSeriesCacheDto updatedSeries = BmwSeriesCacheDto.builder()
                .code("3")
                .name("Seria 3")
                .displayOrder(1)
                .isActive(true)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(bmwCacheInternalService.syncSeries(any(BmwSeriesSyncRequestDto.class)))
                .thenReturn(updatedSeries);

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("3"))
                .andExpect(jsonPath("$.data.name").value("Seria 3"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void syncGeneration_ShouldReturnUpdatedGeneration_WhenValidRequest() throws Exception {
        // Given
        BmwGenerationSyncRequestDto request = BmwGenerationSyncRequestDto.builder()
                .code("F30")
                .seriesCode("3")
                .name("F30/F31/F34/F35")
                .yearStart(2012)
                .yearEnd(2019)
                .bodyCodes(Arrays.asList("F30", "F31", "F34", "F35"))
                .isActive(true)
                .build();

        BmwGenerationCacheDto updatedGeneration = BmwGenerationCacheDto.builder()
                .code("F30")
                .seriesCode("3")
                .name("F30/F31/F34/F35")
                .yearStart(2012)
                .yearEnd(2019)
                .bodyCodes(Arrays.asList("F30", "F31", "F34", "F35"))
                .isActive(true)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(bmwCacheInternalService.syncGeneration(any(BmwGenerationSyncRequestDto.class)))
                .thenReturn(updatedGeneration);

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("F30"))
                .andExpect(jsonPath("$.data.seriesCode").value("3"))
                .andExpect(jsonPath("$.data.yearStart").value(2012))
                .andExpect(jsonPath("$.data.yearEnd").value(2019));
    }

    @Test
    void getAllSeries_ShouldReturnSeriesList_WhenDataExists() throws Exception {
        // Given
        List<BmwSeriesCacheDto> seriesList = Arrays.asList(
                BmwSeriesCacheDto.builder()
                        .code("3")
                        .name("Seria 3")
                        .displayOrder(1)
                        .isActive(true)
                        .build(),
                BmwSeriesCacheDto.builder()
                        .code("5")
                        .name("Seria 5")
                        .displayOrder(2)
                        .isActive(true)
                        .build()
        );

        when(bmwCacheInternalService.getAllSeries())
                .thenReturn(seriesList);

        // When & Then
        mockMvc.perform(get("/internal/bmw-cache/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("3"))
                .andExpect(jsonPath("$.data[1].code").value("5"));
    }

    @Test
    void getSeriesByCode_ShouldReturnSeries_WhenExists() throws Exception {
        // Given
        String seriesCode = "3";
        BmwSeriesCacheDto series = BmwSeriesCacheDto.builder()
                .code("3")
                .name("Seria 3")
                .displayOrder(1)
                .isActive(true)
                .build();

        when(bmwCacheInternalService.getSeriesByCode(eq(seriesCode)))
                .thenReturn(series);

        // When & Then
        mockMvc.perform(get("/internal/bmw-cache/series/{code}", seriesCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("3"))
                .andExpect(jsonPath("$.data.name").value("Seria 3"));
    }

    @Test
    void getAllGenerations_ShouldReturnGenerationsList_WhenDataExists() throws Exception {
        // Given
        List<BmwGenerationCacheDto> generationsList = Arrays.asList(
                BmwGenerationCacheDto.builder()
                        .code("F30")
                        .seriesCode("3")
                        .name("F30/F31/F34/F35")
                        .yearStart(2012)
                        .yearEnd(2019)
                        .isActive(true)
                        .build(),
                BmwGenerationCacheDto.builder()
                        .code("G20")
                        .seriesCode("3")
                        .name("G20/G21")
                        .yearStart(2019)
                        .yearEnd(null)
                        .isActive(true)
                        .build()
        );

        when(bmwCacheInternalService.getAllGenerations())
                .thenReturn(generationsList);

        // When & Then
        mockMvc.perform(get("/internal/bmw-cache/generations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("F30"))
                .andExpect(jsonPath("$.data[1].code").value("G20"));
    }

    @Test
    void getGenerationByCode_ShouldReturnGeneration_WhenExists() throws Exception {
        // Given
        String generationCode = "F30";
        BmwGenerationCacheDto generation = BmwGenerationCacheDto.builder()
                .code("F30")
                .seriesCode("3")
                .name("F30/F31/F34/F35")
                .yearStart(2012)
                .yearEnd(2019)
                .isActive(true)
                .build();

        when(bmwCacheInternalService.getGenerationByCode(eq(generationCode)))
                .thenReturn(generation);

        // When & Then
        mockMvc.perform(get("/internal/bmw-cache/generations/{code}", generationCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("F30"))
                .andExpect(jsonPath("$.data.seriesCode").value("3"));
    }

    @Test
    void getGenerationsBySeriesCode_ShouldReturnGenerationsList_WhenSeriesExists() throws Exception {
        // Given
        String seriesCode = "3";
        List<BmwGenerationCacheDto> generationsList = Arrays.asList(
                BmwGenerationCacheDto.builder()
                        .code("E90")
                        .seriesCode("3")
                        .name("E90/E91/E92/E93")
                        .yearStart(2005)
                        .yearEnd(2012)
                        .isActive(true)
                        .build(),
                BmwGenerationCacheDto.builder()
                        .code("F30")
                        .seriesCode("3")
                        .name("F30/F31/F34/F35")
                        .yearStart(2012)
                        .yearEnd(2019)
                        .isActive(true)
                        .build()
        );

        when(bmwCacheInternalService.getGenerationsBySeriesCode(eq(seriesCode)))
                .thenReturn(generationsList);

        // When & Then
        mockMvc.perform(get("/internal/bmw-cache/series/{seriesCode}/generations", seriesCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("E90"))
                .andExpect(jsonPath("$.data[1].code").value("F30"));
    }

    @Test
    void bulkSync_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        // Given
        BmwCacheBulkSyncRequestDto request = BmwCacheBulkSyncRequestDto.builder()
                .seriesData(Arrays.asList(
                        BmwSeriesSyncRequestDto.builder()
                                .code("3")
                                .name("Seria 3")
                                .displayOrder(1)
                                .isActive(true)
                                .build()
                ))
                .generationData(Arrays.asList(
                        BmwGenerationSyncRequestDto.builder()
                                .code("F30")
                                .seriesCode("3")
                                .name("F30/F31/F34/F35")
                                .yearStart(2012)
                                .yearEnd(2019)
                                .isActive(true)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Bulk sync completed successfully"));
    }

    @Test
    void bulkSync_ShouldReturnBadRequest_WhenEmptyData() throws Exception {
        // Given
        BmwCacheBulkSyncRequestDto request = BmwCacheBulkSyncRequestDto.builder()
                .seriesData(Collections.emptyList())
                .generationData(Collections.emptyList())
                .build();

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Bulk sync completed successfully"));
    }

    @Test
    void syncSeries_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given - missing required fields
        BmwSeriesSyncRequestDto request = BmwSeriesSyncRequestDto.builder()
                .name("Seria 3")
                .build();

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void syncGeneration_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given - missing required fields
        BmwGenerationSyncRequestDto request = BmwGenerationSyncRequestDto.builder()
                .name("F30/F31/F34/F35")
                .build();

        // When & Then
        mockMvc.perform(post("/internal/bmw-cache/sync/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
