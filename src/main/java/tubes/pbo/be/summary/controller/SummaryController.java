package tubes.pbo.be.summary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tubes.pbo.be.shared.dto.ApiResponse;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.security.SecurityContextHelper;
import tubes.pbo.be.summary.dto.SummaryListItem;
import tubes.pbo.be.summary.dto.SummaryResponse;
import tubes.pbo.be.summary.service.SummaryService;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
@Tag(name = "Summarization", description = "PDF summarization endpoints")
public class SummaryController {

    private final SummaryService summaryService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Generate summary",
            description = "Upload a PDF file and generate an AI-powered summary",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<SummaryResponse>> generateSummary(
            @Parameter(
                    description = "PDF file to summarize",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file) {
        Long userId = securityContextHelper.getCurrentUserId();
        SummaryResponse response = summaryService.createSummary(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Summary generated successfully", response));
    }

    @GetMapping
    @Operation(
            summary = "List summaries",
            description = "Get paginated list of summaries for authenticated user",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<PageResponse<SummaryListItem>> listSummaries(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable) {
        Long userId = securityContextHelper.getCurrentUserId();
        Page<SummaryListItem> summaries = summaryService.listSummaries(userId, pageable);
        return ResponseEntity.ok(new PageResponse<>(summaries));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get summary detail",
            description = "Get full summary details including summary text",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummaryDetail(
            @Parameter(description = "Summary ID") @PathVariable Long id) {
        Long userId = securityContextHelper.getCurrentUserId();
        SummaryResponse response = summaryService.getSummaryDetail(userId, id);
        return ResponseEntity.ok(new ApiResponse<>("Summary retrieved successfully", response));
    }
}
