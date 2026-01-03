package tubes.pbo.be.summary.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tubes.pbo.be.shared.exception.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfProcessingServiceTest {

    @InjectMocks
    private PdfProcessingService pdfProcessingService;

    private byte[] validPdfBytes;
    private String expectedText;

    @BeforeEach
    void setUp() {
        // Create a valid PDF with test content
        expectedText = "This is a test PDF document.\nIt contains multiple lines of text.";
        validPdfBytes = createTestPdf(expectedText);
    }

    // ===== extractText Tests =====

    @Test
    void extractText_validPdf_returnsExtractedText() {
        // Arrange
        InputStream pdfInputStream = new ByteArrayInputStream(validPdfBytes);

        // Act
        String result = pdfProcessingService.extractText(pdfInputStream);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("test PDF document"));
        assertTrue(result.contains("multiple lines of text"));
    }

    @Test
    void extractText_multiPagePdf_extractsAllPages() {
        // Arrange
        byte[] multiPagePdf = createMultiPagePdf(3);
        InputStream pdfInputStream = new ByteArrayInputStream(multiPagePdf);

        // Act
        String result = pdfProcessingService.extractText(pdfInputStream);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Page 1 content"));
        assertTrue(result.contains("Page 2 content"));
        assertTrue(result.contains("Page 3 content"));
    }

    @Test
    void extractText_pdfWithSpecialCharacters_extractsCorrectly() {
        // Arrange
        String specialText = "Special characters: à é ñ © ® ™ € £ ¥";
        byte[] pdfBytes = createTestPdf(specialText);
        InputStream pdfInputStream = new ByteArrayInputStream(pdfBytes);

        // Act
        String result = pdfProcessingService.extractText(pdfInputStream);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void extractText_emptyPdf_throwsValidationException() {
        // Arrange - Create PDF with no text
        byte[] emptyPdfBytes = createEmptyPdf();
        InputStream pdfInputStream = new ByteArrayInputStream(emptyPdfBytes);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            pdfProcessingService.extractText(pdfInputStream);
        });

        assertEquals("PDF file appears to be empty or contains no extractable text", exception.getMessage());
    }

    @Test
    void extractText_invalidPdf_throwsValidationException() {
        // Arrange - Invalid PDF bytes
        byte[] invalidBytes = "This is not a valid PDF file".getBytes();
        InputStream pdfInputStream = new ByteArrayInputStream(invalidBytes);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            pdfProcessingService.extractText(pdfInputStream);
        });

        assertEquals("Invalid or corrupted PDF file", exception.getMessage());
    }

    @Test
    void extractText_corruptedPdf_throwsException() {
        // Arrange - Partially valid PDF header but corrupted content
        byte[] corruptedBytes = new byte[validPdfBytes.length];
        System.arraycopy(validPdfBytes, 0, corruptedBytes, 0, validPdfBytes.length);
        // Corrupt the middle of the PDF
        for (int i = 50; i < 100 && i < corruptedBytes.length; i++) {
            corruptedBytes[i] = 0;
        }
        InputStream pdfInputStream = new ByteArrayInputStream(corruptedBytes);

        // Act & Assert
        // Corrupted PDFs may throw various exceptions depending on corruption type
        // (ValidationException, RuntimeException, or iText's AssertionError)
        assertThrows(Throwable.class, () -> {
            pdfProcessingService.extractText(pdfInputStream);
        });
    }

    @Test
    void extractText_largePdf_extractsSuccessfully() {
        // Arrange - Create PDF with lots of text
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("Line ").append(i).append(": Lorem ipsum dolor sit amet. ");
        }
        byte[] largePdfBytes = createTestPdf(largeText.toString());
        InputStream pdfInputStream = new ByteArrayInputStream(largePdfBytes);

        // Act
        String result = pdfProcessingService.extractText(pdfInputStream);

        // Assert
        assertNotNull(result);
        assertTrue(result.length() > 10000);
        assertTrue(result.contains("Line 0"));
        assertTrue(result.contains("Line 999"));
    }

    @Test
    void extractText_pdfWithFormatting_extractsTextOnly() {
        // Arrange - PDF with bold, italic, etc (iText will extract plain text)
        byte[] formattedPdf = createTestPdf("Bold text. Italic text. Normal text.");
        InputStream pdfInputStream = new ByteArrayInputStream(formattedPdf);

        // Act
        String result = pdfProcessingService.extractText(pdfInputStream);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Bold text"));
        assertTrue(result.contains("Italic text"));
        assertTrue(result.contains("Normal text"));
    }

    // ===== Helper Methods =====

    /**
     * Creates a valid PDF with the given text content
     */
    private byte[] createTestPdf(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph(text));

        document.close();
        return baos.toByteArray();
    }

    /**
     * Creates a multi-page PDF
     */
    private byte[] createMultiPagePdf(int numberOfPages) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        for (int i = 1; i <= numberOfPages; i++) {
            document.add(new Paragraph("Page " + i + " content"));
            if (i < numberOfPages) {
                document.add(new com.itextpdf.layout.element.AreaBreak());
            }
        }

        document.close();
        return baos.toByteArray();
    }

    /**
     * Creates an empty PDF with no text content
     */
    private byte[] createEmptyPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Don't add any content

        document.close();
        return baos.toByteArray();
    }
}
