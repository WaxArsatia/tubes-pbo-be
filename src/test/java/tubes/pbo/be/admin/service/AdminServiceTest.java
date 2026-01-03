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
import org.springframework.data.domain.Pageable;
import tubes.pbo.be.admin.dto.ActivityLogResponse;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.model.User.UserRole;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser1;
    private User testUser2;
    private Summary summary1;
    private Summary summary2;
    private Summary summary3;
    private Pageable pageable;

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

        // Setup test summaries
        summary1 = new Summary();
        summary1.setId(1L);
        summary1.setUserId(1L);
        summary1.setOriginalFilename("document1.pdf");
        summary1.setFilePath("uploads/pdfs/1/uuid1.pdf");
        summary1.setSummaryText("Summary 1 text");
        summary1.setAiProvider("gemini");
        summary1.setAiModel("gemini-1.5-flash");
        summary1.setCreatedAt(LocalDateTime.now().minusHours(1));

        summary2 = new Summary();
        summary2.setId(2L);
        summary2.setUserId(2L);
        summary2.setOriginalFilename("document2.pdf");
        summary2.setFilePath("uploads/pdfs/2/uuid2.pdf");
        summary2.setSummaryText("Summary 2 text");
        summary2.setAiProvider("gemini");
        summary2.setAiModel("gemini-1.5-flash");
        summary2.setCreatedAt(LocalDateTime.now().minusHours(2));

        summary3 = new Summary();
        summary3.setId(3L);
        summary3.setUserId(1L);
        summary3.setOriginalFilename("document3.pdf");
        summary3.setFilePath("uploads/pdfs/1/uuid3.pdf");
        summary3.setSummaryText("Summary 3 text");
        summary3.setAiProvider("openai");
        summary3.setAiModel("gpt-4");
        summary3.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Setup pageable
        pageable = PageRequest.of(0, 50);
    }

    // ===== getUserActivity Tests =====

    @Test
    void getUserActivity_withSummaries_returnsActivityLog() {
        // Arrange
        List<Summary> summaries = Arrays.asList(summary1, summary2, summary3);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());

        // Verify first activity log
        ActivityLogResponse log1 = result.getContent().get(0);
        assertEquals(1L, log1.getUserId());
        assertEquals("Test User 1", log1.getUserName());
        assertEquals("user1@example.com", log1.getUserEmail());
        assertEquals("document1.pdf", log1.getOriginalFilename());
        assertEquals("gemini", log1.getAiProvider());
        assertNotNull(log1.getCreatedAt());

        // Verify second activity log
        ActivityLogResponse log2 = result.getContent().get(1);
        assertEquals(2L, log2.getUserId());
        assertEquals("Test User 2", log2.getUserName());
        assertEquals("user2@example.com", log2.getUserEmail());
        assertEquals("document2.pdf", log2.getOriginalFilename());
        assertEquals("gemini", log2.getAiProvider());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
        verify(userRepository, times(2)).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void getUserActivity_noSummaries_returnsEmptyPage() {
        // Arrange
        Page<Summary> summaryPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getUserActivity_deletedUser_filtersNullEntries() {
        // Arrange
        List<Summary> summaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.empty()); // User deleted

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        // Total elements should be 2 (page metadata from repository, not filtered)
        // But content is filtered, so we adjust total to match filtered count
        assertEquals(1, result.getTotalElements());
        // Content should only have 1 entry (filtered out null)
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getUserId());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void getUserActivity_pagination_respectsPageable() {
        // Arrange
        Pageable customPageable = PageRequest.of(1, 10); // Page 1, size 10
        List<Summary> summaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(summaries, customPageable, 25); // Total 25 items

        when(summaryRepository.findAllOrderByCreatedAtDesc(customPageable)).thenReturn(summaryPage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(customPageable);

        // Assert
        assertNotNull(result);
        assertEquals(25, result.getTotalElements()); // Total from repository
        assertEquals(2, result.getContent().size()); // Current page content
        assertEquals(1, result.getNumber()); // Page number
        assertEquals(10, result.getSize()); // Page size

        verify(summaryRepository).findAllOrderByCreatedAtDesc(customPageable);
    }

    @Test
    void getUserActivity_orderedByCreatedAtDesc_maintainsOrder() {
        // Arrange - Summaries should be ordered from newest to oldest
        List<Summary> summaries = Arrays.asList(summary1, summary2, summary3);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        
        // Verify order is maintained (newest first)
        List<ActivityLogResponse> logs = result.getContent();
        assertTrue(logs.get(0).getCreatedAt().isAfter(logs.get(1).getCreatedAt()));
        assertTrue(logs.get(1).getCreatedAt().isAfter(logs.get(2).getCreatedAt()));

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
    }

    @Test
    void getUserActivity_includesAllRequiredFields() {
        // Arrange
        List<Summary> summaries = Arrays.asList(summary1);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        ActivityLogResponse log = result.getContent().get(0);
        assertNotNull(log.getUserId());
        assertNotNull(log.getUserName());
        assertNotNull(log.getUserEmail());
        assertNotNull(log.getOriginalFilename());
        assertNotNull(log.getAiProvider());
        assertNotNull(log.getCreatedAt());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserActivity_multipleSummariesSameUser_includesAllEntries() {
        // Arrange - User 1 has two summaries
        List<Summary> summaries = Arrays.asList(summary1, summary3);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        
        // Both entries should be from user1
        assertEquals(1L, result.getContent().get(0).getUserId());
        assertEquals(1L, result.getContent().get(1).getUserId());
        
        // But different filenames
        assertEquals("document1.pdf", result.getContent().get(0).getOriginalFilename());
        assertEquals("document3.pdf", result.getContent().get(1).getOriginalFilename());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
        verify(userRepository, times(2)).findById(1L);
    }

    @Test
    void getUserActivity_differentAiProviders_showsCorrectProvider() {
        // Arrange
        List<Summary> summaries = Arrays.asList(summary1, summary3);
        Page<Summary> summaryPage = new PageImpl<>(summaries, pageable, summaries.size());

        when(summaryRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(summaryPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        
        // Verify different AI providers
        assertEquals("gemini", result.getContent().get(0).getAiProvider());
        assertEquals("openai", result.getContent().get(1).getAiProvider());

        verify(summaryRepository).findAllOrderByCreatedAtDesc(pageable);
    }

    @Test
    void getUserActivity_largeDataset_paginatesCorrectly() {
        // Arrange
        Pageable smallPageable = PageRequest.of(0, 2); // Small page size
        List<Summary> summaries = Arrays.asList(summary1, summary2);
        Page<Summary> summaryPage = new PageImpl<>(summaries, smallPageable, 100); // Many total items

        when(summaryRepository.findAllOrderByCreatedAtDesc(smallPageable)).thenReturn(summaryPage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser1));

        // Act
        Page<ActivityLogResponse> result = adminService.getUserActivity(smallPageable);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(50, result.getTotalPages()); // 100 / 2

        verify(summaryRepository).findAllOrderByCreatedAtDesc(smallPageable);
    }
}
