package tubes.pbo.be.summary.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tubes.pbo.be.shared.exception.ValidationException;

import java.io.InputStream;

@Service
@Slf4j
public class PdfProcessingService {

    public String extractText(InputStream pdfInputStream) {
        try {
            PdfReader reader = new PdfReader(pdfInputStream);
            PdfDocument pdfDoc = new PdfDocument(reader);
            
            StringBuilder text = new StringBuilder();
            int numberOfPages = pdfDoc.getNumberOfPages();
            
            for (int i = 1; i <= numberOfPages; i++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                text.append(pageText).append("\n");
            }
            
            pdfDoc.close();
            
            String extractedText = text.toString().trim();
            
            if (extractedText.isEmpty()) {
                throw new ValidationException("PDF file appears to be empty or contains no extractable text");
            }
            
            log.info("Successfully extracted {} pages of text from PDF", numberOfPages);
            return extractedText;
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF", e);
            throw new ValidationException("Invalid or corrupted PDF file");
        }
    }
}
