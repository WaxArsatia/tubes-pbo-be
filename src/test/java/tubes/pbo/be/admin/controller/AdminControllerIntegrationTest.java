package tubes.pbo.be.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.admin.dto.*;
import tubes.pbo.be.admin.service.AdminService;
import tubes.pbo.be.admin.service.DashboardService;
import tubes.pbo.be.admin.service.UserManagementService;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.shared.security.SecurityContextHelper;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    private User adminUser;
    private User regularUser;
    private UserDetailResponse userDetailResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private DashboardStatsResponse dashboardStatsResponse;

    @BeforeEach
    void setUp() {
        // Setup admin user
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setRole(User.UserRole.ADMIN);

        // Setup regular user
        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setEmail("user@example.com");
        regularUser.setName("Regular User");
        regularUser.setRole(User.UserRole.USER);

        // Setup user detail response
        userDetailResponse = UserDetailResponse.builder()
                .id(2L)
                .email("user@example.com")
                .name("Regular User")
                .role("USER")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .totalSummaries(5L)
                .build();

        // Setup create user request
        createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setPassword("password123");
        createUserRequest.setName("New User");
        createUserRequest.setRole("USER");

        // Setup update user request
        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("updated@example.com");
        updateUserRequest.setName("Updated Name");
        updateUserRequest.setRole("USER");
        updateUserRequest.setIsVerified(true);

        // Setup dashboard stats response
        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("USER", 5L);
        usersByRole.put("ADMIN", 2L);

        Map<String, Long> aiProviderUsage = new HashMap<>();
        aiProviderUsage.put("gemini", 10L);
        aiProviderUsage.put("openai", 3L);

        List<RecentActivityItem> recentActivity = Arrays.asList(
                RecentActivityItem.builder()
                        .userId(2L)
                        .userName("User 1")
                        .action("Generated summary")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        dashboardStatsResponse = DashboardStatsResponse.builder()
                .totalUsers(7L)
                .totalSummaries(13L)
                .totalActiveUsers(5L)
                .summariesToday(2L)
                .summariesThisWeek(8L)
                .summariesThisMonth(13L)
                .usersByRole(usersByRole)
                .aiProviderUsage(aiProviderUsage)
                .recentActivity(recentActivity)
                .build();

        // Mock security context to return admin ID
        when(securityContextHelper.getCurrentUserId()).thenReturn(1L);
        when(securityContextHelper.getCurrentUser()).thenReturn(adminUser);
    }

    // ===== GET /api/admin/users Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void listUsers_asAdmin_returns200() throws Exception {
        // Arrange
        List<UserDetailResponse> users = Arrays.asList(userDetailResponse);
        Page<UserDetailResponse> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);
        
        when(userManagementService.listUsers(isNull(), any())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.content[0].totalSummaries").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userManagementService).listUsers(isNull(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void listUsers_withSearch_returns200() throws Exception {
        // Arrange
        List<UserDetailResponse> users = Arrays.asList(userDetailResponse);
        Page<UserDetailResponse> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);
        
        when(userManagementService.listUsers(eq("test"), any())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .param("search", "test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userManagementService).listUsers(eq("test"), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void listUsers_withPagination_returns200() throws Exception {
        // Arrange
        List<UserDetailResponse> users = Arrays.asList(userDetailResponse);
        Page<UserDetailResponse> userPage = new PageImpl<>(users, PageRequest.of(1, 10), 25);
        
        when(userManagementService.listUsers(isNull(), any())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25));

        verify(userManagementService).listUsers(isNull(), any());
    }

    // ===== GET /api/admin/users/{id} Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getUserDetail_asAdmin_returns200() throws Exception {
        // Arrange
        when(userManagementService.getUserDetail(2L)).thenReturn(userDetailResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.totalSummaries").value(5));

        verify(userManagementService).getUserDetail(2L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getUserDetail_notFound_returns404() throws Exception {
        // Arrange
        when(userManagementService.getUserDetail(999L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userManagementService).getUserDetail(999L);
    }

    // ===== POST /api/admin/users Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void createUser_validRequest_returns201() throws Exception {
        // Arrange
        UserDetailResponse createdUser = UserDetailResponse.builder()
                .id(3L)
                .email(createUserRequest.getEmail())
                .name(createUserRequest.getName())
                .role("USER")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .totalSummaries(0L)
                .build();

        when(userManagementService.createUser(any(CreateUserRequest.class))).thenReturn(createdUser);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.isVerified").value(true));

        verify(userManagementService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void createUser_duplicateEmail_returns400() throws Exception {
        // Arrange
        when(userManagementService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new ValidationException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userManagementService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void createUser_invalidRequest_returns400() throws Exception {
        // Arrange - Invalid request (missing required fields)
        createUserRequest.setEmail(""); // Invalid

        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());
    }

    // ===== PUT /api/admin/users/{id} Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void updateUser_validRequest_returns200() throws Exception {
        // Arrange
        UserDetailResponse updatedUser = UserDetailResponse.builder()
                .id(2L)
                .email(updateUserRequest.getEmail())
                .name(updateUserRequest.getName())
                .role("USER")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .totalSummaries(5L)
                .build();

        when(userManagementService.updateUser(eq(2L), any(UpdateUserRequest.class)))
                .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        verify(userManagementService).updateUser(eq(2L), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void updateUser_notFound_returns404() throws Exception {
        // Arrange
        when(userManagementService.updateUser(eq(999L), any(UpdateUserRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isNotFound());

        verify(userManagementService).updateUser(eq(999L), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void updateUser_duplicateEmail_returns400() throws Exception {
        // Arrange
        when(userManagementService.updateUser(eq(2L), any(UpdateUserRequest.class)))
                .thenThrow(new ValidationException("Email already exists"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userManagementService).updateUser(eq(2L), any(UpdateUserRequest.class));
    }

    // ===== DELETE /api/admin/users/{id} Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void deleteUser_validRequest_returns200() throws Exception {
        // Arrange
        doNothing().when(userManagementService).deleteUser(1L, 2L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userManagementService).deleteUser(1L, 2L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void deleteUser_selfDelete_returns403() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Cannot delete your own account"))
                .when(userManagementService).deleteUser(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cannot delete your own account"));

        verify(userManagementService).deleteUser(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userManagementService).deleteUser(1L, 999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userManagementService).deleteUser(1L, 999L);
    }

    // ===== GET /api/admin/dashboard Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getDashboardStats_asAdmin_returns200() throws Exception {
        // Arrange
        when(dashboardService.getDashboardStats()).thenReturn(dashboardStatsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Dashboard statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalUsers").value(7))
                .andExpect(jsonPath("$.data.totalSummaries").value(13))
                .andExpect(jsonPath("$.data.totalActiveUsers").value(5))
                .andExpect(jsonPath("$.data.summariesToday").value(2))
                .andExpect(jsonPath("$.data.summariesThisWeek").value(8))
                .andExpect(jsonPath("$.data.summariesThisMonth").value(13))
                .andExpect(jsonPath("$.data.usersByRole.USER").value(5))
                .andExpect(jsonPath("$.data.usersByRole.ADMIN").value(2))
                .andExpect(jsonPath("$.data.aiProviderUsage.gemini").value(10))
                .andExpect(jsonPath("$.data.aiProviderUsage.openai").value(3))
                .andExpect(jsonPath("$.data.recentActivity").isArray())
                .andExpect(jsonPath("$.data.recentActivity[0].userId").value(2));

        verify(dashboardService).getDashboardStats();
    }

    // ===== GET /api/admin/activity Tests =====

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getUserActivity_asAdmin_returns200() throws Exception {
        // Arrange
        List<ActivityLogResponse> activities = Arrays.asList(
                ActivityLogResponse.builder()
                        .userId(2L)
                        .userName("User 1")
                        .userEmail("user1@example.com")
                        .originalFilename("file1.pdf")
                        .aiProvider("gemini")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        Page<ActivityLogResponse> activityPage = new PageImpl<>(activities, PageRequest.of(0, 50), 1);
        
        when(adminService.getUserActivity(any())).thenReturn(activityPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userId").value(2))
                .andExpect(jsonPath("$.content[0].userName").value("User 1"))
                .andExpect(jsonPath("$.content[0].userEmail").value("user1@example.com"))
                .andExpect(jsonPath("$.content[0].originalFilename").value("file1.pdf"))
                .andExpect(jsonPath("$.content[0].aiProvider").value("gemini"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(adminService).getUserActivity(any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getUserActivity_withPagination_returns200() throws Exception {
        // Arrange
        List<ActivityLogResponse> activities = Arrays.asList();
        Page<ActivityLogResponse> activityPage = new PageImpl<>(activities, PageRequest.of(2, 25), 100);
        
        when(adminService.getUserActivity(any())).thenReturn(activityPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/activity")
                .param("page", "2")
                .param("size", "25")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(100));

        verify(adminService).getUserActivity(any());
    }

    // ===== Authorization Tests (Non-Admin) =====

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void listUsers_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).listUsers(any(), any());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void getUserDetail_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).getUserDetail(anyLong());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void createUser_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).createUser(any());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void updateUser_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).updateUser(anyLong(), any());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void deleteUser_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).deleteUser(anyLong(), anyLong());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void getDashboardStats_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(dashboardService, never()).getDashboardStats();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void getUserActivity_asNonAdmin_returns403() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUser()).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(get("/api/admin/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(adminService, never()).getUserActivity(any());
    }

    // ===== Unauthenticated Tests =====

    @Test
    void listUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardStats_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
