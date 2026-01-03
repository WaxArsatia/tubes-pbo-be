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
@Schema(description = "Recent activity item for dashboard")
public class RecentActivityItem {
    
    @Schema(description = "User ID", example = "1")
    private Long userId;
    
    @Schema(description = "User's name", example = "John Doe")
    private String userName;
    
    @Schema(description = "Action performed", example = "Generated summary")
    private String action;
    
    @Schema(description = "Timestamp of action", example = "2026-01-02T09:30:00Z")
    private LocalDateTime timestamp;
}
