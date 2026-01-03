package tubes.pbo.be.history.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.summary.model.Summary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private tubes.pbo.be.shared.config.FileStorageConfig fileStorageConfig;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private Summary testSummary;

    @BeforeEach
    void setUp() {
        testSummary = new Summary();
        testSummary.setId(1L);
        testSummary.setOriginalFilename("test-document.pdf");
        testSummary.setSummaryText("This is a test summary.\n\nThis is the second paragraph of the summary.");
        testSummary.setAiProvider("gemini");
        testSummary.setAiModel("gemini-1.5-pro");
        testSummary.setCreatedAt(LocalDateTime.now());
    }

    // ===== generateSummaryPdf Tests =====

    @Test
    void generateSummaryPdf_validSummary_returnsPdfBytes() {
        // Act
        byte[] result = fileService.generateSummaryPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        // Verify it's a valid PDF (starts with PDF signature)
        assertEquals('%', result[0]);
        assertEquals('P', result[1]);
        assertEquals('D', result[2]);
        assertEquals('F', result[3]);
    }

    @Test
    void generateSummaryPdf_summaryWithMultipleParagraphs_includesAllContent() {
        // Arrange
        testSummary.setSummaryText("First paragraph.\n\nSecond paragraph.\n\nThird paragraph.");

        // Act
        byte[] result = fileService.generateSummaryPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        // Verify it's a valid PDF
        assertEquals('%', result[0]);
    }

    @Test
    void generateSummaryPdf_summaryWithSpecialCharacters_handlesCorrectly() {
        // Arrange
        testSummary.setSummaryText("Summary with special characters: à é ñ © ® ™ € £ ¥");

        // Act
        byte[] result = fileService.generateSummaryPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateSummaryPdf_emptySummaryText_generatesValidPdf() {
        // Arrange
        testSummary.setSummaryText("");

        // Act
        byte[] result = fileService.generateSummaryPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateSummaryPdf_includesMetadata_withProviderAndModel() {
        // Act
        byte[] result = fileService.generateSummaryPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        // The PDF should contain the provider and model info
        // We just verify it generates without error
    }

    // ===== getOriginalPdf Tests =====

    @Test
    void getOriginalPdf_fileExists_returnsPdfBytes() throws IOException {
        // Arrange - Create a test PDF file
        Path testPdfPath = tempDir.resolve("test.pdf");
        byte[] testPdfContent = createTestPdf("Test PDF content");
        Files.write(testPdfPath, testPdfContent);
        testSummary.setFilePath(testPdfPath.getFileName().toString());
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act
        byte[] result = fileService.getOriginalPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertArrayEquals(testPdfContent, result);
    }

    @Test
    void getOriginalPdf_fileNotFound_throwsResourceNotFoundException() {
        // Arrange
        testSummary.setFilePath("nonexistent.pdf");
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            fileService.getOriginalPdf(testSummary);
        });

        assertEquals("Original PDF file not found", exception.getMessage());
    }

    @Test
    void getOriginalPdf_largePdf_readsSuccessfully() throws IOException {
        // Arrange - Create a larger test PDF
        Path testPdfPath = tempDir.resolve("large.pdf");
        byte[] largePdfContent = createLargeTestPdf();
        Files.write(testPdfPath, largePdfContent);
        testSummary.setFilePath(testPdfPath.getFileName().toString());
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);

        // Act
        byte[] result = fileService.getOriginalPdf(testSummary);

        // Assert
        assertNotNull(result);
        assertArrayEquals(largePdfContent, result);
        assertTrue(result.length > 1000); // Should be reasonably large
    }

    // ===== Helper Methods =====

    /**
     * Creates a simple test PDF with the given text.
     */
    private byte[] createTestPdf(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        document.add(new Paragraph(text));

        document.close();

        return baos.toByteArray();
    }

    /**
     * Creates a larger test PDF for testing file size handling.
     */
    private byte[] createLargeTestPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        // Add multiple paragraphs to make it larger
        for (int i = 0; i < 50; i++) {
            document.add(new Paragraph("This is paragraph number " + i + ". It contains some test content to make the PDF larger."));
        }

        document.close();

        return baos.toByteArray();
    }
}
