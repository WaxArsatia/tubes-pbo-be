package tubes.pbo.be.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tubes.pbo.be.admin.dto.DashboardStatsResponse;
import tubes.pbo.be.admin.dto.RecentActivityItem;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    
    /**
     * Get comprehensive dashboard statistics for admin monitoring
     */
    public DashboardStatsResponse getDashboardStats() {
        // Calculate total users
        long totalUsers = userRepository.count();
        
        // Calculate total summaries
        long totalSummaries = summaryRepository.count();
        
        // Calculate total active users (users who have created at least one summary)
        long totalActiveUsers = summaryRepository.countDistinctUsers();
        
        // Calculate summaries by time period
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        
        long summariesToday = summaryRepository.countByCreatedAtAfter(today);
        long summariesThisWeek = summaryRepository.countByCreatedAtAfter(startOfWeek);
        long summariesThisMonth = summaryRepository.countByCreatedAtAfter(startOfMonth);
        
        // Calculate users by role
        Map<String, Long> usersByRole = new HashMap<>();
        List<Object[]> roleStats = userRepository.countUsersByRole();
        for (Object[] row : roleStats) {
            User.UserRole role = (User.UserRole) row[0];
            Long count = (Long) row[1];
            usersByRole.put(role.name(), count);
        }
        
        // Calculate AI provider usage
        Map<String, Long> aiProviderUsage = new HashMap<>();
        List<Object[]> providerStats = summaryRepository.countByAiProvider();
        for (Object[] row : providerStats) {
            String provider = (String) row[0];
            Long count = (Long) row[1];
            aiProviderUsage.put(provider, count);
        }
        
        // Get recent activity (last 10 summaries)
        List<RecentActivityItem> recentActivity = getRecentActivity();
        
        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalSummaries(totalSummaries)
                .totalActiveUsers(totalActiveUsers)
                .summariesToday(summariesToday)
                .summariesThisWeek(summariesThisWeek)
                .summariesThisMonth(summariesThisMonth)
                .usersByRole(usersByRole)
                .aiProviderUsage(aiProviderUsage)
                .recentActivity(recentActivity)
                .build();
    }
    
    /**
     * Get recent activity (last 10 summaries with user info)
     */
    private List<RecentActivityItem> getRecentActivity() {
        Page<Summary> recentSummaries = summaryRepository.findAllOrderByCreatedAtDesc(
                PageRequest.of(0, 10)
        );
        
        return recentSummaries.getContent().stream()
                .map(summary -> {
                    User user = userRepository.findById(summary.getUserId())
                            .orElse(null);
                    
                    if (user == null) {
                        return null;
                    }
                    
                    return RecentActivityItem.builder()
                            .userId(user.getId())
                            .userName(user.getName())
                            .action("Generated summary")
                            .timestamp(summary.getCreatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
