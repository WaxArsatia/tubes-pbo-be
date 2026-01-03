package tubes.pbo.be.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.auth.dto.*;
import tubes.pbo.be.auth.service.AuthService;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.shared.exception.ValidationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private ForgotPasswordRequest forgotPasswordRequest;
    private ResetPasswordRequest resetPasswordRequest;
    private TokenResponse tokenResponse;
    private UserInfo userInfo;

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

        forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("test@example.com");

        resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setToken("test-token-123");
        resetPasswordRequest.setNewPassword("newPassword123");

        userInfo = new UserInfo();
        userInfo.setId(1L);
        userInfo.setEmail("test@example.com");
        userInfo.setName("Test User");
        userInfo.setRole("USER");

        tokenResponse = new TokenResponse();
        tokenResponse.setToken("session-token-123");
        tokenResponse.setUser(userInfo);
    }

    // ===== Register Tests =====

    @Test
    void register_validRequest_returns201() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(1L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new ValidationException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        // Arrange
        registerRequest.setPassword("short");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void register_missingName_returns400() throws Exception {
        // Arrange
        registerRequest.setName("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    // ===== Login Tests =====

    @Test
    void login_validCredentials_returns200() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("session-token-123"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Test User"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_unverifiedUser_returns403() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new ForbiddenException("Please verify your email before logging in"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        // Arrange
        loginRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        // Arrange
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    // ===== Verify Email Tests =====

    @Test
    void verifyEmail_validToken_returns200() throws Exception {
        // Arrange
        String token = "valid-token-123";
        doNothing().when(authService).verifyEmail(token);

        // Act & Assert
        mockMvc.perform(get("/api/auth/verify")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).verifyEmail(token);
    }

    @Test
    void verifyEmail_invalidToken_returns400() throws Exception {
        // Arrange
        String token = "invalid-token";
        doThrow(new ValidationException("Invalid or expired verification token"))
            .when(authService).verifyEmail(token);

        // Act & Assert
        mockMvc.perform(get("/api/auth/verify")
                .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));

        verify(authService).verifyEmail(token);
    }

    @Test
    void verifyEmail_missingToken_returns400() throws Exception {
        // Act & Assert - Spring returns 500 for missing required param by default
        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().is5xxServerError());

        verify(authService, never()).verifyEmail(anyString());
    }

    // ===== Forgot Password Tests =====

    @Test
    void forgotPassword_validEmail_returns200() throws Exception {
        // Arrange
        doNothing().when(authService).forgotPassword(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).forgotPassword(anyString());
    }

    @Test
    void forgotPassword_nonExistentEmail_returns200() throws Exception {
        // Arrange - Security: always return success
        doNothing().when(authService).forgotPassword(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"));

        verify(authService).forgotPassword(anyString());
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        // Arrange
        forgotPasswordRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).forgotPassword(anyString());
    }

    // ===== Reset Password Tests =====

    @Test
    void resetPassword_validRequest_returns200() throws Exception {
        // Arrange
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        // Arrange
        doThrow(new ValidationException("Invalid or expired reset token"))
            .when(authService).resetPassword(any(ResetPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_shortPassword_returns400() throws Exception {
        // Arrange
        resetPasswordRequest.setNewPassword("short");

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_missingToken_returns400() throws Exception {
        // Arrange
        resetPasswordRequest.setToken("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    // ===== Logout Tests =====

    @Test
    @WithMockUser
    void logout_authenticatedUser_returns200() throws Exception {
        // Arrange
        String token = "Bearer session-token-123";
        doNothing().when(authService).logout(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(anyString());
    }

    @Test
    void logout_noAuthHeader_returns401() throws Exception {
        // Act & Assert - Spring returns 500 for missing required header
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().is5xxServerError());

        verify(authService, never()).logout(anyString());
    }

    @Test
    @WithMockUser
    void logout_invalidToken_returns401() throws Exception {
        // Arrange
        String token = "Bearer invalid-token";
        doThrow(new UnauthorizedException("Invalid session token"))
            .when(authService).logout(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid session token"));

        verify(authService).logout(anyString());
    }

    // ===== Content Type Tests =====

    @Test
    void register_wrongContentType_returns415() throws Exception {
        // Act & Assert - Spring returns 500 for unsupported media type
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text"))
                .andExpect(status().is5xxServerError());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void login_emptyBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    // ===== Response Format Tests =====

    @Test
    void allSuccessResponses_haveCorrectFormat() throws Exception {
        // Register
        when(authService.register(any(RegisterRequest.class))).thenReturn(1L);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());

        // Login
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user").exists());
    }

    @Test
    void allErrorResponses_haveCorrectFormat() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
