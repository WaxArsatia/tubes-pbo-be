package tubes.pbo.be.settings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tubes.pbo.be.auth.service.TokenService;
import tubes.pbo.be.settings.dto.ChangePasswordRequest;
import tubes.pbo.be.settings.dto.ProfileResponse;
import tubes.pbo.be.settings.dto.UpdateProfileRequest;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.model.User.UserRole;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private SettingsService settingsService;

    private User testUser;
    private UpdateProfileRequest updateProfileRequest;
    private ChangePasswordRequest changePasswordRequest;
    private String currentToken;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Setup update profile request
        updateProfileRequest = new UpdateProfileRequest();
        updateProfileRequest.setName("Updated Name");

        // Setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("currentPassword123");
        changePasswordRequest.setNewPassword("newPassword123");

        currentToken = "test-session-token-123";
    }

    // ===== getProfile Tests =====

    @Test
    void getProfile_validUserId_returnsProfileResponse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ProfileResponse response = settingsService.getProfile(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals("USER", response.getRole());
        assertNotNull(response.getCreatedAt());

        verify(userRepository).findById(1L);
    }

    @Test
    void getProfile_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> settingsService.getProfile(999L)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    // ===== updateProfile Tests =====

    @Test
    void updateProfile_validRequest_updatesNameAndReturnsProfile() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ProfileResponse response = settingsService.updateProfile(1L, updateProfileRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Name", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(1L, response.getId());

        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        assertEquals("Updated Name", testUser.getName());
    }

    @Test
    void updateProfile_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> settingsService.updateProfile(999L, updateProfileRequest)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_emptyName_stillUpdates() {
        // Arrange
        updateProfileRequest.setName("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ProfileResponse response = settingsService.updateProfile(1L, updateProfileRequest);

        // Assert
        assertNotNull(response);
        assertEquals("", response.getName());
        verify(userRepository).save(testUser);
    }

    // ===== changePassword Tests =====

    @Test
    void changePassword_validRequest_updatesPasswordAndInvalidatesSessions() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(tokenService).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());

        // Act
        settingsService.changePassword(1L, currentToken, changePasswordRequest);

        // Assert
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword123", "hashedPassword");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
        verify(tokenService).deleteAllUserSessionsExceptCurrent(1L, currentToken);
        assertEquals("newHashedPassword", testUser.getPassword());
    }

    @Test
    void changePassword_incorrectCurrentPassword_throwsValidationException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword123", "hashedPassword")).thenReturn(false);

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> settingsService.changePassword(1L, currentToken, changePasswordRequest)
        );

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword123", "hashedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenService, never()).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());
    }

    @Test
    void changePassword_newPasswordTooShort_throwsValidationException() {
        // Arrange
        changePasswordRequest.setNewPassword("short");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword123", "hashedPassword")).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> settingsService.changePassword(1L, currentToken, changePasswordRequest)
        );

        assertEquals("New password must be at least 8 characters long", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword123", "hashedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenService, never()).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());
    }

    @Test
    void changePassword_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> settingsService.changePassword(999L, currentToken, changePasswordRequest)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenService, never()).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());
    }

    @Test
    void changePassword_minimumValidLength_success() {
        // Arrange
        changePasswordRequest.setNewPassword("12345678"); // exactly 8 characters
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("12345678")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(tokenService).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());

        // Act
        settingsService.changePassword(1L, currentToken, changePasswordRequest);

        // Assert
        verify(passwordEncoder).encode("12345678");
        verify(userRepository).save(testUser);
        verify(tokenService).deleteAllUserSessionsExceptCurrent(1L, currentToken);
    }

    @Test
    void changePassword_sameAsCurrentPassword_stillUpdates() {
        // Arrange
        changePasswordRequest.setNewPassword("samePassword123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("samePassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(tokenService).deleteAllUserSessionsExceptCurrent(anyLong(), anyString());

        // Act
        settingsService.changePassword(1L, currentToken, changePasswordRequest);

        // Assert - No exception should be thrown, password should still be updated
        verify(passwordEncoder).encode("samePassword123");
        verify(userRepository).save(testUser);
        verify(tokenService).deleteAllUserSessionsExceptCurrent(1L, currentToken);
    }
}
