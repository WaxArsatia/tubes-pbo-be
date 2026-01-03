package tubes.pbo.be.history.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.history.dto.HistoryResponse;
import tubes.pbo.be.history.service.HistoryService;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.security.SecurityContextHelper;
import tubes.pbo.be.summary.model.Summary;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    private Long testUserId;
    private Long testSummaryId;
    private Summary testSummary;
    private List<HistoryResponse> historyResponses;
    private byte[] testPdfBytes;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testSummaryId = 100L;

        // Setup test summary
        testSummary = new Summary();
        testSummary.setId(testSummaryId);
        testSummary.setUserId(testUserId);
        testSummary.setOriginalFilename("test-document.pdf");
        testSummary.setFilePath("/uploads/pdfs/1/test.pdf");
        testSummary.setSummaryText("Test summary");
        testSummary.setAiProvider("gemini");
        testSummary.setAiModel("gemini-1.5-pro");
        testSummary.setCreatedAt(LocalDateTime.now());

        // Setup history responses
        HistoryResponse response1 = new HistoryResponse(
                1L,
                "document1.pdf",
                "gemini",
                LocalDateTime.now()
        );

        HistoryResponse response2 = new HistoryResponse(
                2L,
                "document2.pdf",
                "gemini",
                LocalDateTime.now().minusDays(1)
        );

        historyResponses = Arrays.asList(response1, response2);

        // Setup test PDF bytes
        testPdfBytes = new byte[]{37, 80, 68, 70}; // %PDF signature

        // Mock security context
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
    }

    // ===== listHistory Tests =====

    @Test
    @WithMockUser
    void listHistory_authenticated_returns200WithPagedData() throws Exception {
        // Arrange
        Page<HistoryResponse> page = new PageImpl<>(historyResponses, PageRequest.of(0, 10), historyResponses.size());
        PageResponse<HistoryResponse> pageResponse = new PageResponse<>(page);
        when(historyService.listHistory(eq(testUserId), any())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("document1.pdf"))
                .andExpect(jsonPath("$.content[0].aiProvider").value("gemini"))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].originalFilename").value("document2.pdf"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(securityContextHelper).getCurrentUserId();
        verify(historyService).listHistory(eq(testUserId), any());
    }

    @Test
    void listHistory_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isForbidden());

        verify(historyService, never()).listHistory(anyLong(), any());
    }

    @Test
    @WithMockUser
    void listHistory_emptyResult_returns200WithEmptyPage() throws Exception {
        // Arrange
        Page<HistoryResponse> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        PageResponse<HistoryResponse> pageResponse = new PageResponse<>(emptyPage);
        when(historyService.listHistory(eq(testUserId), any())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(historyService).listHistory(eq(testUserId), any());
    }

    @Test
    @WithMockUser
    void listHistory_customPagination_usesCorrectParameters() throws Exception {
        // Arrange
        Page<HistoryResponse> page = new PageImpl<>(historyResponses, PageRequest.of(1, 5), 10);
        PageResponse<HistoryResponse> pageResponse = new PageResponse<>(page);
        when(historyService.listHistory(eq(testUserId), any())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/history")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(historyService).listHistory(eq(testUserId), any());
    }

    // ===== deleteSummary Tests =====

    @Test
    @WithMockUser
    void deleteSummary_validOwner_returns200() throws Exception {
        // Arrange
        doNothing().when(historyService).deleteSummary(testUserId, testSummaryId);

        // Act & Assert
        mockMvc.perform(delete("/api/history/{id}", testSummaryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Summary deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(securityContextHelper).getCurrentUserId();
        verify(historyService).deleteSummary(testUserId, testSummaryId);
    }

    @Test
    void deleteSummary_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/history/{id}", testSummaryId))
                .andExpect(status().isForbidden());

        verify(historyService, never()).deleteSummary(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void deleteSummary_notOwner_returns403() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("You don't have permission to delete this summary"))
                .when(historyService).deleteSummary(testUserId, testSummaryId);

        // Act & Assert
        mockMvc.perform(delete("/api/history/{id}", testSummaryId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You don't have permission to delete this summary"));

        verify(historyService).deleteSummary(testUserId, testSummaryId);
    }

    @Test
    @WithMockUser
    void deleteSummary_summaryNotFound_returns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Summary not found"))
                .when(historyService).deleteSummary(testUserId, testSummaryId);

        // Act & Assert
        mockMvc.perform(delete("/api/history/{id}", testSummaryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Summary not found"));

        verify(historyService).deleteSummary(testUserId, testSummaryId);
    }

    // ===== downloadSummaryPdf Tests =====

    @Test
    @WithMockUser
    void downloadSummaryPdf_validOwner_returns200WithPdf() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId)).thenReturn(testSummary);
        when(historyService.downloadSummaryPdf(testUserId, testSummaryId)).thenReturn(testPdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download", testSummaryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"test-document_summary.pdf\""))
                .andExpect(header().string("Content-Length", String.valueOf(testPdfBytes.length)))
                .andExpect(content().bytes(testPdfBytes));

        verify(securityContextHelper).getCurrentUserId();
        verify(historyService).getSummary(testUserId, testSummaryId);
        verify(historyService).downloadSummaryPdf(testUserId, testSummaryId);
    }

    @Test
    void downloadSummaryPdf_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download", testSummaryId))
                .andExpect(status().isForbidden());

        verify(historyService, never()).downloadSummaryPdf(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void downloadSummaryPdf_notOwner_returns403() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId))
                .thenThrow(new ForbiddenException("You don't have permission to access this summary"));

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download", testSummaryId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You don't have permission to access this summary"));

        verify(historyService).getSummary(testUserId, testSummaryId);
        verify(historyService, never()).downloadSummaryPdf(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void downloadSummaryPdf_summaryNotFound_returns404() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId))
                .thenThrow(new ResourceNotFoundException("Summary not found"));

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download", testSummaryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Summary not found"));

        verify(historyService).getSummary(testUserId, testSummaryId);
    }

    @Test
    @WithMockUser
    void downloadSummaryPdf_filenameWithoutPdfExtension_appendsSummaryCorrectly() throws Exception {
        // Arrange
        testSummary.setOriginalFilename("document-no-ext");
        when(historyService.getSummary(testUserId, testSummaryId)).thenReturn(testSummary);
        when(historyService.downloadSummaryPdf(testUserId, testSummaryId)).thenReturn(testPdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download", testSummaryId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"document-no-ext_summary.pdf\""));

        verify(historyService).downloadSummaryPdf(testUserId, testSummaryId);
    }

    // ===== downloadOriginalPdf Tests =====

    @Test
    @WithMockUser
    void downloadOriginalPdf_validOwner_returns200WithPdf() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId)).thenReturn(testSummary);
        when(historyService.downloadOriginalPdf(testUserId, testSummaryId)).thenReturn(testPdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download-original", testSummaryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"test-document.pdf\""))
                .andExpect(header().string("Content-Length", String.valueOf(testPdfBytes.length)))
                .andExpect(content().bytes(testPdfBytes));

        verify(securityContextHelper).getCurrentUserId();
        verify(historyService).getSummary(testUserId, testSummaryId);
        verify(historyService).downloadOriginalPdf(testUserId, testSummaryId);
    }

    @Test
    void downloadOriginalPdf_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download-original", testSummaryId))
                .andExpect(status().isForbidden());

        verify(historyService, never()).downloadOriginalPdf(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void downloadOriginalPdf_notOwner_returns403() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId))
                .thenThrow(new ForbiddenException("You don't have permission to access this summary"));

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download-original", testSummaryId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You don't have permission to access this summary"));

        verify(historyService).getSummary(testUserId, testSummaryId);
        verify(historyService, never()).downloadOriginalPdf(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void downloadOriginalPdf_summaryNotFound_returns404() throws Exception {
        // Arrange
        when(historyService.getSummary(testUserId, testSummaryId))
                .thenThrow(new ResourceNotFoundException("Summary not found"));

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download-original", testSummaryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Summary not found"));

        verify(historyService).getSummary(testUserId, testSummaryId);
    }

    @Test
    @WithMockUser
    void downloadOriginalPdf_preservesOriginalFilename_returnsCorrectFilename() throws Exception {
        // Arrange
        testSummary.setOriginalFilename("my-original-document.pdf");
        when(historyService.getSummary(testUserId, testSummaryId)).thenReturn(testSummary);
        when(historyService.downloadOriginalPdf(testUserId, testSummaryId)).thenReturn(testPdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/{id}/download-original", testSummaryId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"my-original-document.pdf\""));

        verify(historyService).downloadOriginalPdf(testUserId, testSummaryId);
    }
}
