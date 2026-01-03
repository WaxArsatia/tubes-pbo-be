package tubes.pbo.be.history.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * History response DTO (same structure as SummaryResponse).
 * Used for listing summaries in the history endpoint.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "History item response")
public class HistoryResponse {

    @Schema(description = "Summary ID", example = "1")
    private Long id;

    @Schema(description = "Original PDF filename", example = "document.pdf")
    private String originalFilename;

    @Schema(description = "AI provider used", example = "gemini")
    private String aiProvider;

    @Schema(description = "Creation timestamp", example = "2026-01-02T10:00:00Z")
    private LocalDateTime createdAt;
}
