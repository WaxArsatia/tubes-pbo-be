package tubes.pbo.be.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User activity log entry showing summary creation")
public class ActivityLogResponse {
    
    @Schema(description = "User ID", example = "1")
    private Long userId;
    
    @Schema(description = "User's name", example = "John Doe")
    private String userName;
    
    @Schema(description = "User's email", example = "user@example.com")
    private String userEmail;
    
    @Schema(description = "Original PDF filename", example = "document.pdf")
    private String originalFilename;
    
    @Schema(description = "AI provider used", example = "gemini")
    private String aiProvider;
    
    @Schema(description = "Summary creation timestamp", example = "2026-01-02T10:30:00Z")
    private LocalDateTime createdAt;
}
