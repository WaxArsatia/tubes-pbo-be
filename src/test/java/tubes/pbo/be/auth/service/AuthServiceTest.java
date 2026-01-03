package tubes.pbo.be.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tubes.pbo.be.auth.dto.*;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.model.User.UserRole;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        // Setup test data
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());

        testToken = UUID.randomUUID().toString();
    }

    // ===== Register Tests =====

    @Test
    void register_success_createsUserAndSendsVerificationEmail() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenService.generateVerificationToken(anyLong())).thenReturn(testToken);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        Long userId = authService.register(registerRequest);

        // Assert
        assertNotNull(userId);
        assertEquals(1L, userId);
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(tokenService).generateVerificationToken(1L);
        verify(emailService).sendVerificationEmail("test@example.com", testToken);
    }

    @Test
    void register_duplicateEmail_throwsValidationException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> authService.register(registerRequest));
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void register_createsUserWithCorrectDefaultValues() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(tokenService.generateVerificationToken(anyLong())).thenReturn(testToken);
        
        // Capture the saved user
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals("test@example.com", savedUser.getEmail());
            assertEquals("hashedPassword", savedUser.getPassword());
            assertEquals("Test User", savedUser.getName());
            assertEquals(UserRole.USER, savedUser.getRole());
            assertEquals(false, savedUser.getIsVerified());
            // createdAt is set by @PrePersist, so it may be null here
            
            savedUser.setId(1L);
            return savedUser;
        });

        // Act
        authService.register(registerRequest);

        // Assert
        verify(userRepository).save(any(User.class));
    }

    // ===== Login Tests =====

    @Test
    void login_success_returnsTokenResponse() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenService.generateSessionToken(anyLong())).thenReturn(testToken);

        // Act
        TokenResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getName());
        assertEquals("USER", response.getUser().getRole());
        
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(tokenService).generateSessionToken(1L);
    }

    @Test
    void login_invalidEmail_throwsUnauthorizedException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.login(loginRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).generateSessionToken(anyLong());
    }

    @Test
    void login_invalidPassword_throwsUnauthorizedException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.login(loginRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(anyString());
        verify(passwordEncoder).matches(anyString(), anyString());
        verify(tokenService, never()).generateSessionToken(anyLong());
    }

    @Test
    void login_unverifiedUser_throwsForbiddenException() {
        // Arrange
        testUser.setIsVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
            () -> authService.login(loginRequest));
        
        assertEquals("Please verify your email before logging in", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(tokenService, never()).generateSessionToken(anyLong());
    }

    @Test
    void login_caseInsensitiveEmail_success() {
        // Arrange
        loginRequest.setEmail("TEST@EXAMPLE.COM");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenService.generateSessionToken(anyLong())).thenReturn(testToken);

        // Act
        TokenResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository).findByEmail("test@example.com");
    }

    // ===== Verify Email Tests =====

    @Test
    void verifyEmail_success_marksUserAsVerified() {
        // Arrange
        when(tokenService.validateVerificationToken(anyString())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.verifyEmail(testToken);

        // Assert
        verify(tokenService).validateVerificationToken(testToken);
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(user -> user.getIsVerified() == true));
    }

    @Test
    void verifyEmail_invalidToken_throwsValidationException() {
        // Arrange
        when(tokenService.validateVerificationToken(anyString()))
            .thenThrow(new ValidationException("Invalid or expired verification token"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.verifyEmail(testToken));
        
        assertEquals("Invalid or expired verification token", exception.getMessage());
        verify(tokenService).validateVerificationToken(testToken);
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyEmail_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(tokenService.validateVerificationToken(anyString())).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.verifyEmail(testToken));
        
        assertEquals("User not found", exception.getMessage());
        verify(tokenService).validateVerificationToken(testToken);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== Forgot Password Tests =====

    @Test
    void forgotPassword_existingEmail_sendsResetEmail() {
        // Arrange
        String email = "test@example.com";
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tokenService.generatePasswordResetToken(anyLong())).thenReturn(testToken);
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Act
        authService.forgotPassword(email);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(tokenService).generatePasswordResetToken(1L);
        verify(emailService).sendPasswordResetEmail("test@example.com", testToken);
    }

    @Test
    void forgotPassword_nonExistingEmail_doesNotThrowError() {
        // Arrange - Security: don't reveal if email exists
        String email = "nonexistent@example.com";
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        authService.forgotPassword(email);

        // Assert
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(tokenService, never()).generatePasswordResetToken(anyLong());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // ===== Reset Password Tests =====

    @Test
    void resetPassword_success_updatesPasswordAndInvalidatesSessions() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(testToken);
        request.setNewPassword("newPassword123");
        
        when(tokenService.validatePasswordResetToken(anyString())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(tokenService).deleteAllUserSessions(anyLong());

        // Act
        authService.resetPassword(request);

        // Assert
        verify(tokenService).validatePasswordResetToken(testToken);
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(user -> user.getPassword().equals("newHashedPassword")));
        verify(tokenService).deleteAllUserSessions(1L);
    }

    @Test
    void resetPassword_invalidToken_throwsValidationException() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(testToken);
        request.setNewPassword("newPassword123");
        
        when(tokenService.validatePasswordResetToken(anyString()))
            .thenThrow(new ValidationException("Invalid or expired reset token"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.resetPassword(request));
        
        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(tokenService).validatePasswordResetToken(testToken);
        verify(userRepository, never()).findById(anyLong());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenService, never()).deleteAllUserSessions(anyLong());
    }

    @Test
    void resetPassword_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(testToken);
        request.setNewPassword("newPassword123");
        
        when(tokenService.validatePasswordResetToken(anyString())).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.resetPassword(request));
        
        assertEquals("User not found", exception.getMessage());
        verify(tokenService).validatePasswordResetToken(testToken);
        verify(userRepository).findById(999L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenService, never()).deleteAllUserSessions(anyLong());
    }

    // ===== Logout Tests =====

    @Test
    void logout_success_deletesSession() {
        // Arrange
        doNothing().when(tokenService).deleteSession(anyString());

        // Act
        authService.logout(testToken);

        // Assert
        verify(tokenService).deleteSession(testToken);
    }

    @Test
    void logout_invalidToken_throwsUnauthorizedException() {
        // Arrange
        doThrow(new UnauthorizedException("Invalid session token"))
            .when(tokenService).deleteSession(anyString());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> authService.logout(testToken));
        
        assertEquals("Invalid session token", exception.getMessage());
        verify(tokenService).deleteSession(testToken);
    }
}
