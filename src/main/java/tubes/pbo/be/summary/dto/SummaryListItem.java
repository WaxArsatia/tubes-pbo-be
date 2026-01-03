package tubes.pbo.be.summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Summary list item")
public class SummaryListItem {

    @Schema(description = "Summary ID", example = "1")
    private Long id;

    @Schema(description = "Original PDF filename", example = "document.pdf")
    private String originalFilename;

    @Schema(description = "AI provider used", example = "gemini")
    private String aiProvider;

    @Schema(description = "Creation timestamp", example = "2026-01-02T10:00:00Z")
    private LocalDateTime createdAt;
}
