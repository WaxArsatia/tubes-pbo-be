package tubes.pbo.be.summary.service;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import tubes.pbo.be.shared.config.FileStorageConfig;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.summary.dto.SummaryListItem;
import tubes.pbo.be.summary.dto.SummaryResponse;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private PdfProcessingService pdfProcessingService;

    @Mock
    private AiService aiService;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @InjectMocks
    private SummaryService summaryService;

    @TempDir
    Path tempDir;

    private Long testUserId;
    private MultipartFile validPdfFile;
    private Summary testSummary;
    private String extractedText;
    private String generatedSummary;

    @BeforeEach
    void setUp() {
        // Set max file size via reflection
        ReflectionTestUtils.setField(summaryService, "maxFileSize", "10MB");

        testUserId = 1L;
        extractedText = "This is extracted text from the PDF document.";
        generatedSummary = "## Summary\n\nThis is a generated summary.";

        // Create valid PDF file
        validPdfFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        // Create test summary entity
        testSummary = new Summary();
        testSummary.setId(1L);
        testSummary.setUserId(testUserId);
        testSummary.setOriginalFilename("test-document.pdf");
        testSummary.setFilePath("1/uuid-123.pdf");
        testSummary.setSummaryText(generatedSummary);
        testSummary.setAiProvider("gemini");
        testSummary.setAiModel("gemini-1.5-pro");
        testSummary.setCreatedAt(LocalDateTime.now());

        // Mock file storage config - lenient to avoid UnnecessaryStubbingException
        lenient().when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);
    }

    // ===== createSummary Tests =====

    @Test
    void createSummary_validPdf_createsSuccessfully() {
        // Arrange
        when(pdfProcessingService.extractText(any())).thenReturn(extractedText);
        when(aiService.generateSummary(extractedText)).thenReturn(generatedSummary);
        when(aiService.getAiProvider()).thenReturn("gemini");
        when(aiService.getAiModel()).thenReturn("gemini-1.5-pro");
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // Act
        SummaryResponse result = summaryService.createSummary(testUserId, validPdfFile);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-document.pdf", result.getOriginalFilename());
        assertEquals(generatedSummary, result.getSummaryText());
        assertEquals("gemini", result.getAiProvider());
        assertEquals("gemini-1.5-pro", result.getAiModel());
        assertNotNull(result.getCreatedAt());

        verify(pdfProcessingService).extractText(any());
        verify(aiService).generateSummary(extractedText);
        verify(summaryRepository).save(any(Summary.class));
    }

    @Test
    void createSummary_emptyFile_throwsValidationException() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            summaryService.createSummary(testUserId, emptyFile);
        });

        assertEquals("File is required", exception.getMessage());
        verify(pdfProcessingService, never()).extractText(any());
        verify(aiService, never()).generateSummary(any());
        verify(summaryRepository, never()).save(any());
    }

    @Test
    void createSummary_invalidContentType_throwsValidationException() {
        // Arrange
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "Not a PDF".getBytes()
        );

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            summaryService.createSummary(testUserId, invalidFile);
        });

        assertEquals("Only PDF files are allowed", exception.getMessage());
        verify(pdfProcessingService, never()).extractText(any());
    }

    @Test
    void createSummary_invalidFileExtension_throwsValidationException() {
        // Arrange
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.txt",
                "application/pdf",  // Correct content type
                "Content".getBytes()
        );

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            summaryService.createSummary(testUserId, invalidFile);
        });

        assertEquals("Only PDF files are allowed", exception.getMessage());
    }

    @Test
    void createSummary_nullContentType_throwsValidationException() {
        // Arrange
        MultipartFile fileWithNullContentType = new MockMultipartFile(
                "file",
                "document.pdf",
                null,  // Null content type
                "Content".getBytes()
        );

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            summaryService.createSummary(testUserId, fileWithNullContentType);
        });

        assertEquals("Only PDF files are allowed", exception.getMessage());
    }

    @Test
    void createSummary_pdfExtractionFails_throwsValidationException() {
        // Arrange
        when(pdfProcessingService.extractText(any()))
                .thenThrow(new ValidationException("Invalid or corrupted PDF file"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            summaryService.createSummary(testUserId, validPdfFile);
        });

        assertEquals("Invalid or corrupted PDF file", exception.getMessage());
        verify(pdfProcessingService).extractText(any());
        verify(aiService, never()).generateSummary(any());
        verify(summaryRepository, never()).save(any());
    }

    @Test
    void createSummary_aiServiceFails_throwsRuntimeException() {
        // Arrange
        when(pdfProcessingService.extractText(any())).thenReturn(extractedText);
        when(aiService.generateSummary(extractedText))
                .thenThrow(new RuntimeException("Failed to generate summary. Please try again later."));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            summaryService.createSummary(testUserId, validPdfFile);
        });

        assertTrue(exception.getMessage().contains("Failed to create summary"));
        verify(pdfProcessingService).extractText(any());
        verify(aiService).generateSummary(extractedText);
        verify(summaryRepository, never()).save(any());
    }

    @Test
    void createSummary_fileStorageFails_throwsRuntimeException() throws Exception {
        // Arrange
        MultipartFile failingFile = mock(MultipartFile.class);
        when(failingFile.isEmpty()).thenReturn(false);
        when(failingFile.getContentType()).thenReturn("application/pdf");
        when(failingFile.getOriginalFilename()).thenReturn("test.pdf");
        when(failingFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("PDF".getBytes()))
                .thenThrow(new IOException("Failed to read file"));

        // Lenient because test fails before these are called
        lenient().when(pdfProcessingService.extractText(any())).thenReturn(extractedText);
        lenient().when(aiService.generateSummary(extractedText)).thenReturn(generatedSummary);
        lenient().when(aiService.getAiProvider()).thenReturn("gemini");
        lenient().when(aiService.getAiModel()).thenReturn("gemini-1.5-pro");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            summaryService.createSummary(testUserId, failingFile);
        });

        assertTrue(exception.getMessage().contains("Failed to save file"));
        verify(summaryRepository, never()).save(any());
    }

    @Test
    void createSummary_uppercasePdfExtension_acceptsFile() {
        // Arrange
        MultipartFile uppercaseFile = new MockMultipartFile(
                "file",
                "DOCUMENT.PDF",
                "application/pdf",
                "PDF content".getBytes()
        );

        when(pdfProcessingService.extractText(any())).thenReturn(extractedText);
        when(aiService.generateSummary(extractedText)).thenReturn(generatedSummary);
        when(aiService.getAiProvider()).thenReturn("gemini");
        when(aiService.getAiModel()).thenReturn("gemini-1.5-pro");
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // Act
        SummaryResponse result = summaryService.createSummary(testUserId, uppercaseFile);

        // Assert
        assertNotNull(result);
        verify(pdfProcessingService).extractText(any());
        verify(summaryRepository).save(any(Summary.class));
    }

    // ===== listSummaries Tests =====

    @Test
    void listSummaries_withResults_returnsPaginatedList() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Summary> summaries = Arrays.asList(testSummary);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, 1);

        when(summaryRepository.findByUserId(testUserId, pageable)).thenReturn(summaryPage);

        // Act
        Page<SummaryListItem> result = summaryService.listSummaries(testUserId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        SummaryListItem item = result.getContent().get(0);
        assertEquals(testSummary.getId(), item.getId());
        assertEquals(testSummary.getOriginalFilename(), item.getOriginalFilename());
        assertEquals(testSummary.getAiProvider(), item.getAiProvider());
        assertEquals(testSummary.getCreatedAt(), item.getCreatedAt());

        verify(summaryRepository).findByUserId(testUserId, pageable);
    }

    @Test
    void listSummaries_noResults_returnsEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Summary> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(summaryRepository.findByUserId(testUserId, pageable)).thenReturn(emptyPage);

        // Act
        Page<SummaryListItem> result = summaryService.listSummaries(testUserId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());

        verify(summaryRepository).findByUserId(testUserId, pageable);
    }

    @Test
    void listSummaries_multipleSummaries_returnsAllItems() {
        // Arrange
        Summary summary2 = new Summary();
        summary2.setId(2L);
        summary2.setUserId(testUserId);
        summary2.setOriginalFilename("document2.pdf");
        summary2.setAiProvider("gemini");
        summary2.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        List<Summary> summaries = Arrays.asList(testSummary, summary2);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, 2);

        when(summaryRepository.findByUserId(testUserId, pageable)).thenReturn(summaryPage);

        // Act
        Page<SummaryListItem> result = summaryService.listSummaries(testUserId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(summaryRepository).findByUserId(testUserId, pageable);
    }

    // ===== getSummaryDetail Tests =====

    @Test
    void getSummaryDetail_validId_returnsDetail() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(1L, testUserId))
                .thenReturn(Optional.of(testSummary));

        // Act
        SummaryResponse result = summaryService.getSummaryDetail(testUserId, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(testSummary.getId(), result.getId());
        assertEquals(testSummary.getOriginalFilename(), result.getOriginalFilename());
        assertEquals(testSummary.getSummaryText(), result.getSummaryText());
        assertEquals(testSummary.getAiProvider(), result.getAiProvider());
        assertEquals(testSummary.getAiModel(), result.getAiModel());
        assertEquals(testSummary.getCreatedAt(), result.getCreatedAt());

        verify(summaryRepository).findByIdAndUserId(1L, testUserId);
    }

    @Test
    void getSummaryDetail_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(summaryRepository.findByIdAndUserId(999L, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            summaryService.getSummaryDetail(testUserId, 999L);
        });

        assertTrue(exception.getMessage().contains("Summary"));
        assertTrue(exception.getMessage().contains("999"));

        verify(summaryRepository).findByIdAndUserId(999L, testUserId);
    }

    @Test
    void getSummaryDetail_wrongUser_throwsResourceNotFoundException() {
        // Arrange
        Long wrongUserId = 999L;
        when(summaryRepository.findByIdAndUserId(1L, wrongUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            summaryService.getSummaryDetail(wrongUserId, 1L);
        });

        verify(summaryRepository).findByIdAndUserId(1L, wrongUserId);
    }
}
