package tubes.pbo.be.history.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tubes.pbo.be.history.dto.HistoryResponse;
import tubes.pbo.be.quiz.repository.QuizRepository;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private FileService fileService;

    @Mock
    private tubes.pbo.be.shared.config.FileStorageConfig fileStorageConfig;

    @InjectMocks
    private HistoryService historyService;

    @TempDir
    Path tempDir;

    private Long testUserId;
    private Long testSummaryId;
    private Summary testSummary;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testSummaryId = 100L;
        pageable = PageRequest.of(0, 10);

        testSummary = new Summary();
        testSummary.setId(testSummaryId);
        testSummary.setUserId(testUserId);
        testSummary.setOriginalFilename("test-document.pdf");
        testSummary.setFilePath(tempDir.resolve("test.pdf").toString());
        testSummary.setSummaryText("Test summary text");
        testSummary.setAiProvider("gemini");
        testSummary.setAiModel("gemini-1.5-pro");
        testSummary.setCreatedAt(LocalDateTime.now());
    }

    // ===== listHistory Tests =====

    @Test
    void listHistory_validUser_returnsPagedHistory() {
        // Arrange
        Summary summary1 = new Summary();
        summary1.setId(1L);
        summary1.setOriginalFilename("doc1.pdf");
        summary1.setAiProvider("gemini");
        summary1.setCreatedAt(LocalDateTime.now());

        Summary summary2 = new Summary();
        summary2.setId(2L);
        summary2.setOriginalFilename("doc2.pdf");
        summary2.setAiProvider("gemini");
        summary2.setCreatedAt(LocalDateTime.now().minusDays(1));

        List<Summary> summaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());
        when(summaryRepository.findByUserId(testUserId, pageable)).thenReturn(summaryPage);

        // Act
        PageResponse<HistoryResponse> result = historyService.listHistory(testUserId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals("doc1.pdf", result.getContent().get(0).getOriginalFilename());
        assertEquals("doc2.pdf", result.getContent().get(1).getOriginalFilename());

        verify(summaryRepository).findByUserId(testUserId, pageable);
    }

    @Test
    void listHistory_emptyResult_returnsEmptyPage() {
        // Arrange
        Page<Summary> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        when(summaryRepository.findByUserId(testUserId, pageable)).thenReturn(emptyPage);

        // Act
        PageResponse<HistoryResponse> result = historyService.listHistory(testUserId, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(summaryRepository).findByUserId(testUserId, pageable);
    }

    // ===== deleteSummary Tests =====

    @Test
    void deleteSummary_validOwner_deletesSuccessfully() throws IOException {
        // Arrange - Create actual file
        Path testFile = Path.of(testSummary.getFilePath());
        Files.createFile(testFile);
        assertTrue(testFile.toFile().exists());

        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act
        historyService.deleteSummary(testUserId, testSummaryId);

        // Assert
        assertFalse(testFile.toFile().exists()); // File should be deleted
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(quizRepository).deleteBySummaryId(testSummaryId);
        verify(summaryRepository).delete(testSummary);
    }

    @Test
    void deleteSummary_fileNotExists_stillDeletesRecord() {
        // Arrange - Don't create file
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act
        historyService.deleteSummary(testUserId, testSummaryId);

        // Assert
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(quizRepository).deleteBySummaryId(testSummaryId);
        verify(summaryRepository).delete(testSummary);
    }

    @Test
    void deleteSummary_notOwner_throwsForbiddenException() {
        // Arrange
        Long otherUserId = 999L;
        when(summaryRepository.findByIdAndUserId(testSummaryId, otherUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            historyService.deleteSummary(otherUserId, testSummaryId);
        });

        assertEquals("You don't have permission to delete this summary", exception.getMessage());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, otherUserId);
        verify(summaryRepository).existsById(testSummaryId);
        verify(quizRepository, never()).deleteBySummaryId(any());
        verify(summaryRepository, never()).delete(any());
    }

    @Test
    void deleteSummary_summaryNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            historyService.deleteSummary(testUserId, testSummaryId);
        });

        assertEquals("Summary not found", exception.getMessage());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(summaryRepository).existsById(testSummaryId);
        verify(quizRepository, never()).deleteBySummaryId(any());
        verify(summaryRepository, never()).delete(any());
    }

    @Test
    void deleteSummary_cascadesDeleteToQuizzes_deletesSuccessfully() throws IOException {
        // Arrange
        Path testFile = Path.of(testSummary.getFilePath());
        Files.createFile(testFile);
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act
        historyService.deleteSummary(testUserId, testSummaryId);

        // Assert
        verify(quizRepository).deleteBySummaryId(testSummaryId); // Quizzes deleted first
        verify(summaryRepository).delete(testSummary); // Then summary
    }

    // ===== downloadSummaryPdf Tests =====

    @Test
    void downloadSummaryPdf_validOwner_returnsPdfBytes() {
        // Arrange
        byte[] expectedPdf = new byte[]{1, 2, 3, 4};
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(fileService.generateSummaryPdf(testSummary)).thenReturn(expectedPdf);

        // Act
        byte[] result = historyService.downloadSummaryPdf(testUserId, testSummaryId);

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(fileService).generateSummaryPdf(testSummary);
    }

    @Test
    void downloadSummaryPdf_notOwner_throwsForbiddenException() {
        // Arrange
        Long otherUserId = 999L;
        when(summaryRepository.findByIdAndUserId(testSummaryId, otherUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            historyService.downloadSummaryPdf(otherUserId, testSummaryId);
        });

        assertEquals("You don't have permission to access this summary", exception.getMessage());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, otherUserId);
        verify(fileService, never()).generateSummaryPdf(any());
    }

    @Test
    void downloadSummaryPdf_summaryNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            historyService.downloadSummaryPdf(testUserId, testSummaryId);
        });

        assertEquals("Summary not found", exception.getMessage());
        verify(fileService, never()).generateSummaryPdf(any());
    }

    // ===== downloadOriginalPdf Tests =====

    @Test
    void downloadOriginalPdf_validOwner_returnsPdfBytes() {
        // Arrange
        byte[] expectedPdf = new byte[]{5, 6, 7, 8};
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(fileService.getOriginalPdf(testSummary)).thenReturn(expectedPdf);

        // Act
        byte[] result = historyService.downloadOriginalPdf(testUserId, testSummaryId);

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(fileService).getOriginalPdf(testSummary);
    }

    @Test
    void downloadOriginalPdf_notOwner_throwsForbiddenException() {
        // Arrange
        Long otherUserId = 999L;
        when(summaryRepository.findByIdAndUserId(testSummaryId, otherUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            historyService.downloadOriginalPdf(otherUserId, testSummaryId);
        });

        assertEquals("You don't have permission to access this summary", exception.getMessage());
        verify(fileService, never()).getOriginalPdf(any());
    }

    @Test
    void downloadOriginalPdf_summaryNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            historyService.downloadOriginalPdf(testUserId, testSummaryId);
        });

        assertEquals("Summary not found", exception.getMessage());
        verify(fileService, never()).getOriginalPdf(any());
    }

    // ===== getSummary Tests =====

    @Test
    void getSummary_validOwner_returnsSummary() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));

        // Act
        Summary result = historyService.getSummary(testUserId, testSummaryId);

        // Assert
        assertNotNull(result);
        assertEquals(testSummaryId, result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("test-document.pdf", result.getOriginalFilename());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
    }

    @Test
    void getSummary_notOwner_throwsForbiddenException() {
        // Arrange
        Long otherUserId = 999L;
        when(summaryRepository.findByIdAndUserId(testSummaryId, otherUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            historyService.getSummary(otherUserId, testSummaryId);
        });

        assertEquals("You don't have permission to access this summary", exception.getMessage());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, otherUserId);
        verify(summaryRepository).existsById(testSummaryId);
    }

    @Test
    void getSummary_summaryNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.empty());
        when(summaryRepository.existsById(testSummaryId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            historyService.getSummary(testUserId, testSummaryId);
        });

        assertEquals("Summary not found", exception.getMessage());
        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(summaryRepository).existsById(testSummaryId);
    }
}
