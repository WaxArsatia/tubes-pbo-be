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
@Schema(description = "Detailed user information with summary count")
public class UserDetailResponse {
    
    @Schema(description = "User ID", example = "1")
    private Long id;
    
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User's full name", example = "John Doe")
    private String name;
    
    @Schema(description = "User role", example = "USER")
    private String role;
    
    @Schema(description = "Whether user's email is verified", example = "true")
    private Boolean isVerified;
    
    @Schema(description = "Account creation timestamp", example = "2026-01-01T00:00:00Z")
    private LocalDateTime createdAt;
    
    @Schema(description = "Total number of summaries created by user", example = "15")
    private Long totalSummaries;
}
