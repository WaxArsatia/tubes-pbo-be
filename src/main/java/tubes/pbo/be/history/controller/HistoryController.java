package tubes.pbo.be.history.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tubes.pbo.be.history.dto.HistoryResponse;
import tubes.pbo.be.history.service.HistoryService;
import tubes.pbo.be.shared.dto.ApiResponse;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.security.SecurityContextHelper;

/**
 * REST controller for managing summary history and file downloads.
 * Provides endpoints for viewing, deleting, and downloading summaries.
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Summary history and file management endpoints")
public class HistoryController {

    private final HistoryService historyService;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping
    @Operation(
            summary = "Get summary history",
            description = "Get paginated list of user's summaries in history view",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<PageResponse<HistoryResponse>> listHistory(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable) {
        Long userId = securityContextHelper.getCurrentUserId();
        PageResponse<HistoryResponse> history = historyService.listHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete summary",
            description = "Delete a summary and all associated data (quizzes, files)",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteSummary(
            @Parameter(description = "Summary ID") @PathVariable Long id) {
        Long userId = securityContextHelper.getCurrentUserId();
        historyService.deleteSummary(userId, id);
        return ResponseEntity.ok(new ApiResponse<>("Summary deleted successfully", null));
    }

    @GetMapping("/{id}/download")
    @Operation(
            summary = "Download summary as PDF",
            description = "Download the generated summary as a PDF file",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<byte[]> downloadSummaryPdf(
            @Parameter(description = "Summary ID") @PathVariable Long id) {
        Long userId = securityContextHelper.getCurrentUserId();
        
        // Get summary metadata for filename
        var summary = historyService.getSummary(userId, id);
        String baseFilename = summary.getOriginalFilename().replace(".pdf", "");
        String filename = baseFilename + "_summary.pdf";
        
        byte[] pdfBytes = historyService.downloadSummaryPdf(userId, id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/download-original")
    @Operation(
            summary = "Download original PDF",
            description = "Download the original uploaded PDF file",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<byte[]> downloadOriginalPdf(
            @Parameter(description = "Summary ID") @PathVariable Long id) {
        Long userId = securityContextHelper.getCurrentUserId();
        
        // Get summary metadata for filename
        var summary = historyService.getSummary(userId, id);
        String filename = summary.getOriginalFilename();
        
        byte[] pdfBytes = historyService.downloadOriginalPdf(userId, id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
