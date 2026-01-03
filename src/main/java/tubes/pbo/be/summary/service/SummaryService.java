package tubes.pbo.be.summary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tubes.pbo.be.shared.config.FileStorageConfig;
import tubes.pbo.be.shared.exception.FileOperationException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.summary.dto.SummaryListItem;
import tubes.pbo.be.summary.dto.SummaryResponse;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final PdfProcessingService pdfProcessingService;
    private final AiService aiService;
    private final FileStorageConfig fileStorageConfig;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Transactional
    public SummaryResponse createSummary(Long userId, MultipartFile file) {
        // Validate file is not empty
        if (file.isEmpty()) {
            throw new ValidationException("File is required");
        }

        // Validate file is PDF
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new ValidationException("Only PDF files are allowed");
        }

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Only PDF files are allowed");
        }

        // Validate file size is handled by Spring Boot multipart config

        try {
            // Extract text from PDF
            String extractedText = pdfProcessingService.extractText(file.getInputStream());

            // Generate summary using AI
            String summaryText = aiService.generateSummary(extractedText);

            // Generate UUID for file storage
            String fileUuid = UUID.randomUUID().toString();
            String storedFilename = fileUuid + ".pdf";

            // Create user directory if not exists
            Path userDir = fileStorageConfig.getUploadPath().resolve(userId.toString());
            Files.createDirectories(userDir);

            // Save file
            Path filePath = userDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create summary entity
            Summary summary = new Summary();
            summary.setUserId(userId);
            summary.setOriginalFilename(originalFilename);
            summary.setFilePath(userId + "/" + storedFilename);
            summary.setSummaryText(summaryText);
            summary.setAiProvider(aiService.getAiProvider());
            summary.setAiModel(aiService.getAiModel());

            summary = summaryRepository.save(summary);

            log.info("Summary created successfully for user {} with ID {}", userId, summary.getId());

            return toResponse(summary);

        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to save PDF file", e);
            throw new FileOperationException("Failed to save file. Please try again.", e);
        } catch (Exception e) {
            log.error("Failed to create summary", e);
            throw new FileOperationException("Failed to create summary. Please try again.", e);
        }
    }

    public Page<SummaryListItem> listSummaries(Long userId, Pageable pageable) {
        return summaryRepository.findByUserId(userId, pageable)
                .map(this::toListItem);
    }

    public SummaryResponse getSummaryDetail(Long userId, Long summaryId) {
        Summary summary = summaryRepository.findByIdAndUserId(summaryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary", "id", summaryId));

        return toResponse(summary);
    }

    private SummaryResponse toResponse(Summary summary) {
        SummaryResponse response = new SummaryResponse();
        response.setId(summary.getId());
        response.setOriginalFilename(summary.getOriginalFilename());
        response.setSummaryText(summary.getSummaryText());
        response.setAiProvider(summary.getAiProvider());
        response.setAiModel(summary.getAiModel());
        response.setCreatedAt(summary.getCreatedAt());
        return response;
    }

    private SummaryListItem toListItem(Summary summary) {
        SummaryListItem item = new SummaryListItem();
        item.setId(summary.getId());
        item.setOriginalFilename(summary.getOriginalFilename());
        item.setAiProvider(summary.getAiProvider());
        item.setCreatedAt(summary.getCreatedAt());
        return item;
    }
}
