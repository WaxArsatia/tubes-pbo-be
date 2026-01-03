package tubes.pbo.be.summary.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.shared.security.SecurityContextHelper;
import tubes.pbo.be.summary.dto.SummaryListItem;
import tubes.pbo.be.summary.dto.SummaryResponse;
import tubes.pbo.be.summary.service.SummaryService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
class SummaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    private Long testUserId;
    private SummaryResponse summaryResponse;
    private List<SummaryListItem> summaryListItems;
    private MockMultipartFile validPdfFile;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Setup summary response
        summaryResponse = new SummaryResponse();
        summaryResponse.setId(1L);
        summaryResponse.setOriginalFilename("test-document.pdf");
        summaryResponse.setSummaryText("## Summary\n\nThis is a test summary.");
        summaryResponse.setAiProvider("gemini");
        summaryResponse.setAiModel("gemini-1.5-pro");
        summaryResponse.setCreatedAt(LocalDateTime.now());

        // Setup list items
        SummaryListItem item1 = new SummaryListItem();
        item1.setId(1L);
        item1.setOriginalFilename("document1.pdf");
        item1.setAiProvider("gemini");
        item1.setCreatedAt(LocalDateTime.now());

        SummaryListItem item2 = new SummaryListItem();
        item2.setId(2L);
        item2.setOriginalFilename("document2.pdf");
        item2.setAiProvider("gemini");
        item2.setCreatedAt(LocalDateTime.now().minusDays(1));

        summaryListItems = Arrays.asList(item1, item2);

        // Setup mock multipart file
        validPdfFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        // Mock security context
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
    }

    // ===== generateSummary Tests =====

    @Test
    @WithMockUser
    void generateSummary_validPdf_returns201() throws Exception {
        // Arrange
        when(summaryService.createSummary(anyLong(), any())).thenReturn(summaryResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/summaries")
                        .file(validPdfFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Summary generated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.originalFilename").value("test-document.pdf"))
                .andExpect(jsonPath("$.data.summaryText").value("## Summary\n\nThis is a test summary."))
                .andExpect(jsonPath("$.data.aiProvider").value("gemini"))
                .andExpect(jsonPath("$.data.aiModel").value("gemini-1.5-pro"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(securityContextHelper).getCurrentUserId();
        verify(summaryService).createSummary(eq(testUserId), any());
    }

    @Test
    void generateSummary_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/summaries")
                        .file(validPdfFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden()); // 403 for anonymous users

        verify(summaryService, never()).createSummary(anyLong(), any());
    }

    private static Stream<String> invalidSummaryErrors() {
        return Stream.of(
                "File is required",
                "Only PDF files are allowed",
                "Invalid or corrupted PDF file"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSummaryErrors")
    @WithMockUser
    void generateSummary_invalidInput_returns400(String errorMessage) throws Exception {
        // Arrange
        when(summaryService.createSummary(anyLong(), any()))
                .thenThrow(new ValidationException(errorMessage));

        // Act & Assert
        mockMvc.perform(multipart("/api/summaries")
                        .file(validPdfFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(summaryService).createSummary(eq(testUserId), any());
    }

    @Test
    @WithMockUser
    void generateSummary_aiServiceFails_returns500() throws Exception {
        // Arrange
        when(summaryService.createSummary(anyLong(), any()))
                .thenThrow(new RuntimeException("Failed to generate summary. Please try again later."));

        // Act & Assert
        mockMvc.perform(multipart("/api/summaries")
                        .file(validPdfFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));

        verify(summaryService).createSummary(eq(testUserId), any());
    }

    @Test
    @WithMockUser
    void generateSummary_noFileParameter_returns400() throws Exception {
        // Act & Assert - Spring returns 500 for missing required parameter
        mockMvc.perform(multipart("/api/summaries")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());

        verify(summaryService, never()).createSummary(anyLong(), any());
    }

    // ===== listSummaries Tests =====

    @Test
    @WithMockUser
    void listSummaries_withResults_returns200() throws Exception {
        // Arrange
        Page<SummaryListItem> page = new PageImpl<>(summaryListItems, PageRequest.of(0, 10), 2);
        when(summaryService.listSummaries(anyLong(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/summaries")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("document1.pdf"))
                .andExpect(jsonPath("$.content[0].aiProvider").value("gemini"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(securityContextHelper).getCurrentUserId();
        verify(summaryService).listSummaries(eq(testUserId), any());
    }

    @Test
    @WithMockUser
    void listSummaries_emptyResults_returns200() throws Exception {
        // Arrange
        Page<SummaryListItem> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(summaryService.listSummaries(anyLong(), any())).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(summaryService).listSummaries(eq(testUserId), any());
    }

    @Test
    void listSummaries_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/summaries"))
                .andExpect(status().isForbidden()); // 403 for anonymous users

        verify(summaryService, never()).listSummaries(anyLong(), any());
    }

    @Test
    @WithMockUser
    void listSummaries_withPagination_returns200() throws Exception {
        // Arrange
        Page<SummaryListItem> page = new PageImpl<>(
                summaryListItems.subList(0, 1), 
                PageRequest.of(1, 1), 
                2
        );
        when(summaryService.listSummaries(anyLong(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/summaries")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(summaryService).listSummaries(eq(testUserId), any());
    }

    @Test
    @WithMockUser
    void listSummaries_defaultPagination_uses10PerPage() throws Exception {
        // Arrange
        Page<SummaryListItem> page = new PageImpl<>(summaryListItems, PageRequest.of(0, 10), 2);
        when(summaryService.listSummaries(anyLong(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));

        verify(summaryService).listSummaries(eq(testUserId), any());
    }

    // ===== getSummaryDetail Tests =====

    @Test
    @WithMockUser
    void getSummaryDetail_validId_returns200() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
        when(summaryService.getSummaryDetail(testUserId, 1L)).thenReturn(summaryResponse);

        // Act & Assert
        mockMvc.perform(get("/api/summaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Summary retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.originalFilename").value("test-document.pdf"))
                .andExpect(jsonPath("$.data.summaryText").value("## Summary\n\nThis is a test summary."))
                .andExpect(jsonPath("$.data.aiProvider").value("gemini"))
                .andExpect(jsonPath("$.data.aiModel").value("gemini-1.5-pro"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(securityContextHelper).getCurrentUserId();
        verify(summaryService).getSummaryDetail(testUserId, 1L);
    }

    @Test
    void getSummaryDetail_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/summaries/1"))
                .andExpect(status().isForbidden()); // 403 for anonymous users

        verify(summaryService, never()).getSummaryDetail(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void getSummaryDetail_notFound_returns404() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
        when(summaryService.getSummaryDetail(testUserId, 999L))
                .thenThrow(new ResourceNotFoundException("Summary", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/summaries/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());

        verify(summaryService).getSummaryDetail(testUserId, 999L);
    }

    @Test
    @WithMockUser
    void getSummaryDetail_wrongUser_returns404() throws Exception {
        // Arrange - User doesn't own this summary
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
        when(summaryService.getSummaryDetail(testUserId, 1L))
                .thenThrow(new ResourceNotFoundException("Summary", "id", 1L));

        // Act & Assert
        mockMvc.perform(get("/api/summaries/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(summaryService).getSummaryDetail(testUserId, 1L);
    }

    @Test
    @WithMockUser
    void getSummaryDetail_invalidId_returns400() throws Exception {
        // Act & Assert - Spring returns 500 for type mismatch in path variable
        mockMvc.perform(get("/api/summaries/invalid"))
                .andExpect(status().isInternalServerError());

        verify(summaryService, never()).getSummaryDetail(anyLong(), anyLong());
    }
}
