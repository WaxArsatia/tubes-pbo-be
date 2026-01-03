package tubes.pbo.be.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tubes.pbo.be.history.dto.HistoryResponse;
import tubes.pbo.be.quiz.repository.QuizRepository;
import tubes.pbo.be.shared.config.FileStorageConfig;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.exception.FileOperationException;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.nio.file.Path;

/**
 * Service for managing summary history and file operations.
 * Reuses SummaryService for listing and provides additional delete/download functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final SummaryRepository summaryRepository;
    private final QuizRepository quizRepository;
    private final FileService fileService;
    private final FileStorageConfig fileStorageConfig;

    /**
     * Lists all summaries for a user (paginated).
     * Reuses SummaryService for listing logic.
     *
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Paginated list of history items
     */
    public PageResponse<HistoryResponse> listHistory(Long userId, Pageable pageable) {
        log.info("Listing history for user ID: {}", userId);
        
        Page<Summary> summaries = summaryRepository.findByUserId(userId, pageable);
        
        Page<HistoryResponse> historyPage = summaries.map(this::toHistoryResponse);
        
        return new PageResponse<>(historyPage);
    }

    /**
     * Deletes a summary and all associated data (quizzes, files).
     *
     * @param userId The user ID (for ownership verification)
     * @param summaryId The summary ID to delete
     * @throws ForbiddenException if user doesn't own the summary
     * @throws ResourceNotFoundException if summary not found
     */
    @Transactional
    public void deleteSummary(Long userId, Long summaryId) {
        log.info("Deleting summary ID: {} for user ID: {}", summaryId, userId);
        
        // Check if summary exists and user owns it
        Summary summary = summaryRepository.findByIdAndUserId(summaryId, userId)
                .orElseThrow(() -> {
                    if (summaryRepository.existsById(summaryId)) {
                        log.warn("User {} attempted to delete summary {} they don't own", userId, summaryId);
                        throw new ForbiddenException("You don't have permission to delete this summary");
                    }
                    log.warn("Summary {} not found", summaryId);
                    throw new ResourceNotFoundException("Summary not found");
                });

        try {
            // Delete associated quizzes (cascade will delete questions)
            log.debug("Deleting quizzes for summary ID: {}", summaryId);
            quizRepository.deleteBySummaryId(summaryId);
            
            // Delete the PDF file from filesystem
            deleteSummaryFile(summary);
            
            // Delete the database record
            summaryRepository.delete(summary);
            
            log.info("Successfully deleted summary ID: {} and all associated data", summaryId);
            
        } catch (Exception e) {
            log.error("Error during summary deletion for ID: {}", summaryId, e);
            throw new FileOperationException("Failed to delete summary: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads the summary as a generated PDF.
     *
     * @param userId The user ID (for ownership verification)
     * @param summaryId The summary ID
     * @return PDF file as byte array
     * @throws ForbiddenException if user doesn't own the summary
     * @throws ResourceNotFoundException if summary not found
     */
    public byte[] downloadSummaryPdf(Long userId, Long summaryId) {
        log.info("Downloading summary PDF for ID: {} by user ID: {}", summaryId, userId);
        
        Summary summary = getSummaryWithOwnershipCheck(userId, summaryId);
        return fileService.generateSummaryPdf(summary);
    }

    /**
     * Downloads the original uploaded PDF file.
     *
     * @param userId The user ID (for ownership verification)
     * @param summaryId The summary ID
     * @return Original PDF file as byte array
     * @throws ForbiddenException if user doesn't own the summary
     * @throws ResourceNotFoundException if summary not found
     */
    public byte[] downloadOriginalPdf(Long userId, Long summaryId) {
        log.info("Downloading original PDF for ID: {} by user ID: {}", summaryId, userId);
        
        Summary summary = getSummaryWithOwnershipCheck(userId, summaryId);
        return fileService.getOriginalPdf(summary);
    }

    /**
     * Gets the summary for a user (for retrieving metadata like filename).
     *
     * @param userId The user ID
     * @param summaryId The summary ID
     * @return The summary entity
     * @throws ForbiddenException if user doesn't own the summary
     * @throws ResourceNotFoundException if summary not found
     */
    public Summary getSummary(Long userId, Long summaryId) {
        return getSummaryWithOwnershipCheck(userId, summaryId);
    }

    /**
     * Gets the summary with ownership check.
     *
     * @param userId The user ID
     * @param summaryId The summary ID
     * @return The summary if found and owned by user
     * @throws ForbiddenException if user doesn't own the summary
     * @throws ResourceNotFoundException if summary not found
     */
    private Summary getSummaryWithOwnershipCheck(Long userId, Long summaryId) {
        return summaryRepository.findByIdAndUserId(summaryId, userId)
                .orElseThrow(() -> {
                    if (summaryRepository.existsById(summaryId)) {
                        log.warn("User {} attempted to access summary {} they don't own", userId, summaryId);
                        throw new ForbiddenException("You don't have permission to access this summary");
                    }
                    log.warn("Summary {} not found", summaryId);
                    throw new ResourceNotFoundException("Summary not found");
                });
    }

    /**
     * Converts a Summary entity to HistoryResponse DTO.
     */
    private HistoryResponse toHistoryResponse(Summary summary) {
        return new HistoryResponse(
                summary.getId(),
                summary.getOriginalFilename(),
                summary.getAiProvider(),
                summary.getCreatedAt()
        );
    }

    /**
     * Deletes the summary file from the filesystem.
     * Logs warnings but doesn't throw exceptions to avoid breaking the deletion flow.
     *
     * @param summary The summary containing the file path
     */
    private void deleteSummaryFile(Summary summary) {
        Path filePath = fileStorageConfig.getUploadPath().resolve(summary.getFilePath());
        if (java.nio.file.Files.exists(filePath)) {
            try {
                java.nio.file.Files.delete(filePath);
                log.debug("Successfully deleted file: {}", summary.getFilePath());
            } catch (java.io.IOException e) {
                log.warn("Failed to delete file: {}", summary.getFilePath(), e);
            }
        } else {
            log.warn("File not found during deletion: {}", summary.getFilePath());
        }
    }
}
