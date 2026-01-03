package tubes.pbo.be.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tubes.pbo.be.admin.dto.DashboardStatsResponse;
import tubes.pbo.be.admin.dto.RecentActivityItem;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.model.User.UserRole;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser1;
    private User testUser2;
    private User adminUser;
    private Summary summary1;
    private Summary summary2;
    private Summary summary3;

    @BeforeEach
    void setUp() {
        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setEmail("user1@example.com");
        testUser1.setName("Test User 1");
        testUser1.setRole(UserRole.USER);
        testUser1.setIsVerified(true);
        testUser1.setCreatedAt(LocalDateTime.now());

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setEmail("user2@example.com");
        testUser2.setName("Test User 2");
        testUser2.setRole(UserRole.USER);
        testUser2.setIsVerified(true);
        testUser2.setCreatedAt(LocalDateTime.now());

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsVerified(true);
        adminUser.setCreatedAt(LocalDateTime.now());

        // Setup test summaries
        summary1 = new Summary();
        summary1.setId(1L);
        summary1.setUserId(1L);
        summary1.setOriginalFilename("file1.pdf");
        summary1.setSummaryText("Summary 1");
        summary1.setAiProvider("gemini");
        summary1.setAiModel("gemini-1.5-flash");
        summary1.setCreatedAt(LocalDateTime.now().minusHours(1));

        summary2 = new Summary();
        summary2.setId(2L);
        summary2.setUserId(2L);
        summary2.setOriginalFilename("file2.pdf");
        summary2.setSummaryText("Summary 2");
        summary2.setAiProvider("gemini");
        summary2.setAiModel("gemini-1.5-flash");
        summary2.setCreatedAt(LocalDateTime.now().minusHours(2));

        summary3 = new Summary();
        summary3.setId(3L);
        summary3.setUserId(1L);
        summary3.setOriginalFilename("file3.pdf");
        summary3.setSummaryText("Summary 3");
        summary3.setAiProvider("openai");
        summary3.setAiModel("gpt-4");
        summary3.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    // ===== getDashboardStats Tests =====

    @Test
    void getDashboardStats_allMetrics_returnsCompleteStats() {
        // Arrange
        when(userRepository.count()).thenReturn(3L);
        when(summaryRepository.count()).thenReturn(5L);
        when(summaryRepository.countDistinctUsers()).thenReturn(2L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(1L) // today
                .thenReturn(2L) // this week
                .thenReturn(3L); // this month

        // Mock users by role
        List<Object[]> roleStats = Arrays.asList(
                new Object[]{UserRole.USER, 2L},
                new Object[]{UserRole.ADMIN, 1L}
        );
        when(userRepository.countUsersByRole()).thenReturn(roleStats);

        // Mock AI provider usage
        List<Object[]> providerStats = Arrays.asList(
                new Object[]{"gemini", 3L},
                new Object[]{"openai", 2L}
        );
        when(summaryRepository.countByAiProvider()).thenReturn(providerStats);

        // Mock recent activity
        List<Summary> recentSummaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(recentSummaries);
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getTotalUsers());
        assertEquals(5L, result.getTotalSummaries());
        assertEquals(2L, result.getTotalActiveUsers());
        assertEquals(1L, result.getSummariesToday());
        assertEquals(2L, result.getSummariesThisWeek());
        assertEquals(3L, result.getSummariesThisMonth());

        // Verify users by role
        Map<String, Long> usersByRole = result.getUsersByRole();
        assertNotNull(usersByRole);
        assertEquals(2L, usersByRole.get("USER"));
        assertEquals(1L, usersByRole.get("ADMIN"));

        // Verify AI provider usage
        Map<String, Long> aiProviderUsage = result.getAiProviderUsage();
        assertNotNull(aiProviderUsage);
        assertEquals(3L, aiProviderUsage.get("gemini"));
        assertEquals(2L, aiProviderUsage.get("openai"));

        // Verify recent activity
        List<RecentActivityItem> recentActivity = result.getRecentActivity();
        assertNotNull(recentActivity);
        assertEquals(2, recentActivity.size());

        verify(userRepository).count();
        verify(summaryRepository).count();
        verify(summaryRepository).countDistinctUsers();
        verify(summaryRepository, times(3)).countByCreatedAtAfter(any(LocalDateTime.class));
        verify(userRepository).countUsersByRole();
        verify(summaryRepository).countByAiProvider();
    }

    @Test
    void getDashboardStats_noUsers_returnsZeroStats() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(summaryRepository.count()).thenReturn(0L);
        when(summaryRepository.countDistinctUsers()).thenReturn(0L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getTotalSummaries());
        assertEquals(0L, result.getTotalActiveUsers());
        assertEquals(0L, result.getSummariesToday());
        assertEquals(0L, result.getSummariesThisWeek());
        assertEquals(0L, result.getSummariesThisMonth());
        assertTrue(result.getUsersByRole().isEmpty());
        assertTrue(result.getAiProviderUsage().isEmpty());
        assertTrue(result.getRecentActivity().isEmpty());
    }

    @Test
    void getDashboardStats_usersByRole_handlesMultipleRoles() {
        // Arrange
        when(userRepository.count()).thenReturn(10L);
        when(summaryRepository.count()).thenReturn(20L);
        when(summaryRepository.countDistinctUsers()).thenReturn(5L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        List<Object[]> roleStats = Arrays.asList(
                new Object[]{UserRole.USER, 8L},
                new Object[]{UserRole.ADMIN, 2L}
        );
        when(userRepository.countUsersByRole()).thenReturn(roleStats);
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        Map<String, Long> usersByRole = result.getUsersByRole();
        assertEquals(2, usersByRole.size());
        assertEquals(8L, usersByRole.get("USER"));
        assertEquals(2L, usersByRole.get("ADMIN"));
    }

    @Test
    void getDashboardStats_aiProviderUsage_handlesMultipleProviders() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        when(summaryRepository.count()).thenReturn(15L);
        when(summaryRepository.countDistinctUsers()).thenReturn(3L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());

        List<Object[]> providerStats = Arrays.asList(
                new Object[]{"gemini", 10L},
                new Object[]{"openai", 3L},
                new Object[]{"anthropic", 2L}
        );
        when(summaryRepository.countByAiProvider()).thenReturn(providerStats);
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        Map<String, Long> aiProviderUsage = result.getAiProviderUsage();
        assertEquals(3, aiProviderUsage.size());
        assertEquals(10L, aiProviderUsage.get("gemini"));
        assertEquals(3L, aiProviderUsage.get("openai"));
        assertEquals(2L, aiProviderUsage.get("anthropic"));
    }

    @Test
    void getDashboardStats_recentActivity_returnsLast10() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        when(summaryRepository.count()).thenReturn(15L);
        when(summaryRepository.countDistinctUsers()).thenReturn(3L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());

        List<Summary> recentSummaries = Arrays.asList(summary1, summary2, summary3);
        Page<Summary> summaryPage = new PageImpl<>(recentSummaries);
        when(summaryRepository.findAllOrderByCreatedAtDesc(PageRequest.of(0, 10)))
                .thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        List<RecentActivityItem> recentActivity = result.getRecentActivity();
        assertNotNull(recentActivity);
        assertEquals(3, recentActivity.size());

        RecentActivityItem activity1 = recentActivity.get(0);
        assertEquals(1L, activity1.getUserId());
        assertEquals("Test User 1", activity1.getUserName());
        assertEquals("Generated summary", activity1.getAction());
        assertNotNull(activity1.getTimestamp());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(PageRequest.of(0, 10));
    }

    @Test
    void getDashboardStats_recentActivity_filtersNullUsers() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        when(summaryRepository.count()).thenReturn(15L);
        when(summaryRepository.countDistinctUsers()).thenReturn(3L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());

        List<Summary> recentSummaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(recentSummaries);
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.empty()); // User deleted

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        List<RecentActivityItem> recentActivity = result.getRecentActivity();
        assertEquals(1, recentActivity.size()); // Only one user found
        assertEquals(1L, recentActivity.get(0).getUserId());
    }

    @Test
    void getDashboardStats_summariesByPeriod_calculatesCorrectly() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        when(summaryRepository.count()).thenReturn(100L);
        when(summaryRepository.countDistinctUsers()).thenReturn(3L);
        
        // Mock summaries by period: today=5, week=20, month=50
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(5L)   // today
                .thenReturn(20L)  // this week
                .thenReturn(50L); // this month

        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        assertEquals(5L, result.getSummariesToday());
        assertEquals(20L, result.getSummariesThisWeek());
        assertEquals(50L, result.getSummariesThisMonth());
        
        // Verify date calculations
        verify(summaryRepository, times(3)).countByCreatedAtAfter(any(LocalDateTime.class));
    }

    @Test
    void getDashboardStats_activeUsers_countsDistinctUsersWithSummaries() {
        // Arrange
        when(userRepository.count()).thenReturn(10L); // Total 10 users
        when(summaryRepository.count()).thenReturn(25L);
        when(summaryRepository.countDistinctUsers()).thenReturn(7L); // Only 7 have summaries
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        assertEquals(10L, result.getTotalUsers());
        assertEquals(7L, result.getTotalActiveUsers()); // Less than total users
        verify(summaryRepository).countDistinctUsers();
    }

    @Test
    void getDashboardStats_recentActivity_hasCorrectActionMessage() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);
        when(summaryRepository.count()).thenReturn(1L);
        when(summaryRepository.countDistinctUsers()).thenReturn(1L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());

        List<Summary> recentSummaries = Arrays.asList(summary1);
        Page<Summary> summaryPage = new PageImpl<>(recentSummaries);
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        List<RecentActivityItem> recentActivity = result.getRecentActivity();
        assertEquals(1, recentActivity.size());
        assertEquals("Generated summary", recentActivity.get(0).getAction());
    }

    @Test
    void getDashboardStats_recentActivity_orderedByCreatedAtDesc() {
        // Arrange
        when(userRepository.count()).thenReturn(2L);
        when(summaryRepository.count()).thenReturn(3L);
        when(summaryRepository.countDistinctUsers()).thenReturn(2L);
        when(summaryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countUsersByRole()).thenReturn(Arrays.asList());
        when(summaryRepository.countByAiProvider()).thenReturn(Arrays.asList());

        // Most recent first
        List<Summary> recentSummaries = Arrays.asList(summary1, summary2, summary3);
        Page<Summary> summaryPage = new PageImpl<>(recentSummaries);
        when(summaryRepository.findAllOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(summaryPage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser1));

        // Act
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Assert
        List<RecentActivityItem> recentActivity = result.getRecentActivity();
        assertEquals(3, recentActivity.size());
        // Verify timestamps are in descending order
        assertTrue(recentActivity.get(0).getTimestamp().isAfter(
                recentActivity.get(1).getTimestamp()));
        assertTrue(recentActivity.get(1).getTimestamp().isAfter(
                recentActivity.get(2).getTimestamp()));
    }
}
