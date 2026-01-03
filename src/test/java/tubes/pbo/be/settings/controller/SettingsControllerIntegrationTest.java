package tubes.pbo.be.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.settings.dto.ChangePasswordRequest;
import tubes.pbo.be.settings.dto.ProfileResponse;
import tubes.pbo.be.settings.dto.UpdateProfileRequest;
import tubes.pbo.be.settings.service.SettingsService;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.shared.security.SecurityContextHelper;

import java.time.LocalDateTime;
import java.util.stream.Stream;

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
class SettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    private ProfileResponse profileResponse;
    private UpdateProfileRequest updateProfileRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        // Setup profile response
        profileResponse = ProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        // Setup update profile request
        updateProfileRequest = new UpdateProfileRequest();
        updateProfileRequest.setName("Updated Name");

        // Setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("currentPassword123");
        changePasswordRequest.setNewPassword("newPassword123");

        // Mock security context to return user ID
        when(securityContextHelper.getCurrentUserId()).thenReturn(1L);
    }

    // ===== GET /api/settings Tests =====

    @Test
    @WithMockUser(username = "1")
    void getProfile_authenticatedUser_returns200() throws Exception {
        // Arrange
        when(settingsService.getProfile(1L)).thenReturn(profileResponse);

        // Act & Assert
        mockMvc.perform(get("/api/settings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(settingsService).getProfile(1L);
    }

    @Test
    void getProfile_unauthenticatedUser_returns403() throws Exception {
        // Act & Assert - Without @WithMockUser, Spring Security blocks with 403
        mockMvc.perform(get("/api/settings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(settingsService, never()).getProfile(anyLong());
    }

    @Test
    @WithMockUser(username = "999")
    void getProfile_userNotFound_returns404() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUserId()).thenReturn(999L);
        when(settingsService.getProfile(999L))
            .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/settings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(settingsService).getProfile(999L);
    }

    // ===== PUT /api/settings Tests =====

    @Test
    @WithMockUser(username = "1")
    void updateProfile_validRequest_returns200() throws Exception {
        // Arrange
        ProfileResponse updatedProfile = ProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("Updated Name")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(settingsService.updateProfile(eq(1L), any(UpdateProfileRequest.class)))
            .thenReturn(updatedProfile);

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Name updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        verify(settingsService).updateProfile(eq(1L), any(UpdateProfileRequest.class));
    }

    @Test
    void updateProfile_unauthenticatedUser_returns403() throws Exception {
        // Act & Assert - Without @WithMockUser, Spring Security blocks with 403
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isForbidden());

        verify(settingsService, never()).updateProfile(anyLong(), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    void updateProfile_emptyName_returns400() throws Exception {
        // Arrange
        updateProfileRequest.setName("");

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1")
    void updateProfile_nullName_returns400() throws Exception {
        // Arrange
        updateProfileRequest.setName(null);

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1")
    void updateProfile_nameTooLong_returns400() throws Exception {
        // Arrange - name longer than 255 characters
        updateProfileRequest.setName("a".repeat(256));

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "999")
    void updateProfile_userNotFound_returns404() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUserId()).thenReturn(999L);
        when(settingsService.updateProfile(eq(999L), any(UpdateProfileRequest.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(settingsService).updateProfile(eq(999L), any(UpdateProfileRequest.class));
    }

    // ===== PUT /api/settings/password Tests =====

    @Test
    @WithMockUser(username = "1")
    void changePassword_validRequest_returns200() throws Exception {
        // Arrange
        doNothing().when(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(settingsService).changePassword(eq(1L), eq("test-token-123"), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_unauthenticatedUser_returns403() throws Exception {
        // Act & Assert - Without @WithMockUser, Spring Security blocks with 403
        mockMvc.perform(put("/api/settings/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isForbidden());

        verify(settingsService, never()).changePassword(anyLong(), anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    void changePassword_incorrectCurrentPassword_returns400() throws Exception {
        // Arrange
        doThrow(new ValidationException("Current password is incorrect"))
            .when(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        verify(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));
    }

    private static Stream<ChangePasswordRequest> invalidPasswordRequests() {
        return Stream.of(
                createRequest(null, "NewPassword123"),  // Null current password
                createRequest("", "NewPassword123"),    // Empty current password
                createRequest("CurrentPassword123", null), // Null new password
                createRequest("CurrentPassword123", ""),   // Empty new password
                createRequest("CurrentPassword123", "short") // New password too short
        );
    }

    private static ChangePasswordRequest createRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordRequests")
    @WithMockUser(username = "1")
    void changePassword_invalidRequests_returns400(ChangePasswordRequest request) throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "999")
    void changePassword_userNotFound_returns404() throws Exception {
        // Arrange
        when(securityContextHelper.getCurrentUserId()).thenReturn(999L);
        doThrow(new ResourceNotFoundException("User not found"))
            .when(settingsService).changePassword(eq(999L), anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(settingsService).changePassword(eq(999L), anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    void changePassword_minimumValidPasswordLength_returns200() throws Exception {
        // Arrange - exactly 8 characters
        changePasswordRequest.setNewPassword("12345678");
        doNothing().when(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    void changePassword_withoutAuthorizationHeader_returns500() throws Exception {
        // Act & Assert - Missing required header returns 500 (MissingRequestHeaderException)
        mockMvc.perform(put("/api/settings/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "1")
    void changePassword_validLongPassword_returns200() throws Exception {
        // Arrange - test with a very long password
        changePasswordRequest.setNewPassword("a".repeat(100));
        doNothing().when(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/settings/password")
                .header("Authorization", "Bearer test-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(settingsService).changePassword(eq(1L), anyString(), any(ChangePasswordRequest.class));
    }
}
