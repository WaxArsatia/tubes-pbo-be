package tubes.pbo.be.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard statistics for admin monitoring")
public class DashboardStatsResponse {
    
    @Schema(description = "Total number of users", example = "42")
    private Long totalUsers;
    
    @Schema(description = "Total number of summaries", example = "150")
    private Long totalSummaries;
    
    @Schema(description = "Number of active users (created at least one summary)", example = "12")
    private Long totalActiveUsers;
    
    @Schema(description = "Number of summaries created today", example = "5")
    private Long summariesToday;
    
    @Schema(description = "Number of summaries created this week", example = "23")
    private Long summariesThisWeek;
    
    @Schema(description = "Number of summaries created this month", example = "98")
    private Long summariesThisMonth;
    
    @Schema(description = "User count by role", example = "{\"USER\": 40, \"ADMIN\": 2}")
    private Map<String, Long> usersByRole;
    
    @Schema(description = "AI provider usage statistics", example = "{\"gemini\": 150}")
    private Map<String, Long> aiProviderUsage;
    
    @Schema(description = "Recent activity (last 10 summaries)")
    private List<RecentActivityItem> recentActivity;
}
