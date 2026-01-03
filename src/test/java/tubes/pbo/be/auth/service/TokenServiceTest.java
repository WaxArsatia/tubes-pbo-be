package tubes.pbo.be.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tubes.pbo.be.auth.model.PasswordResetToken;
import tubes.pbo.be.auth.model.Session;
import tubes.pbo.be.auth.model.VerificationToken;
import tubes.pbo.be.auth.repository.PasswordResetTokenRepository;
import tubes.pbo.be.auth.repository.SessionRepository;
import tubes.pbo.be.auth.repository.VerificationTokenRepository;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private Long testUserId;
    private String testToken;
    private Session testSession;
    private VerificationToken testVerificationToken;
    private PasswordResetToken testPasswordResetToken;

    @BeforeEach
    void setUp() {
        // Set @Value fields that Mockito doesn't inject
        ReflectionTestUtils.setField(tokenService, "sessionExpiryHours", 24);
        ReflectionTestUtils.setField(tokenService, "emailVerificationExpiryHours", 24);
        ReflectionTestUtils.setField(tokenService, "passwordResetExpiryHours", 1);
        
        testUserId = 1L;
        testToken = UUID.randomUUID().toString();

        testSession = new Session();
        testSession.setId(1L);
        testSession.setToken(testToken);
        testSession.setUserId(testUserId);
        testSession.setExpiresAt(LocalDateTime.now().plusHours(24));
        testSession.setCreatedAt(LocalDateTime.now());

        testVerificationToken = new VerificationToken();
        testVerificationToken.setId(1L);
        testVerificationToken.setToken(testToken);
        testVerificationToken.setUserId(testUserId);
        testVerificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        testVerificationToken.setCreatedAt(LocalDateTime.now());
        testVerificationToken.setUsed(false);

        testPasswordResetToken = new PasswordResetToken();
        testPasswordResetToken.setId(1L);
        testPasswordResetToken.setToken(testToken);
        testPasswordResetToken.setUserId(testUserId);
        testPasswordResetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        testPasswordResetToken.setCreatedAt(LocalDateTime.now());
        testPasswordResetToken.setUsed(false);
    }

    // ===== Session Token Tests =====

    @Test
    void generateSessionToken_success_returnsValidToken() {
        // Arrange
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // Act
        String token = tokenService.generateSessionToken(testUserId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(sessionRepository).save(argThat(session -> 
            session.getUserId().equals(testUserId) &&
            session.getToken() != null &&
            session.getExpiresAt() != null
        ));
    }

    @Test
    void generateSessionToken_setsCorrectExpiry() {
        // Arrange
        final LocalDateTime[] capturedExpiresAt = new LocalDateTime[1];
        
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            capturedExpiresAt[0] = session.getExpiresAt();
            session.setId(1L);
            return session;
        });

        // Act
        LocalDateTime beforeGeneration = LocalDateTime.now();
        tokenService.generateSessionToken(testUserId);
        LocalDateTime afterGeneration = LocalDateTime.now();

        // Assert
        verify(sessionRepository).save(any(Session.class));
        assertNotNull(capturedExpiresAt[0]);
        
        // Expiry should be 24 hours from generation time (with tolerance)
        LocalDateTime expectedMin = beforeGeneration.plusHours(24).minusSeconds(1);
        LocalDateTime expectedMax = afterGeneration.plusHours(24).plusSeconds(1);
        assertTrue(capturedExpiresAt[0].isAfter(expectedMin) && capturedExpiresAt[0].isBefore(expectedMax),
            "Expected expiresAt to be ~24 hours from now, but was: " + capturedExpiresAt[0]);
    }

    @Test
    void validateSessionToken_validToken_returnsUserId() {
        // Arrange
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(testSession));

        // Act
        Long userId = tokenService.validateSessionToken(testToken);

        // Assert
        assertEquals(testUserId, userId);
        verify(sessionRepository).findByToken(testToken);
    }

    @Test
    void validateSessionToken_tokenNotFound_throwsUnauthorizedException() {
        // Arrange
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> tokenService.validateSessionToken(testToken));
        
        assertEquals("Invalid or expired session token", exception.getMessage());
        verify(sessionRepository).findByToken(testToken);
    }

    @Test
    void validateSessionToken_expiredToken_throwsUnauthorizedException() {
        // Arrange
        testSession.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(testSession));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> tokenService.validateSessionToken(testToken));
        
        assertEquals("Session has expired", exception.getMessage());
        verify(sessionRepository).findByToken(testToken);
    }

    @Test
    void deleteSession_validToken_deletesSession() {
        // Arrange
        doNothing().when(sessionRepository).deleteByToken(testToken);

        // Act
        tokenService.deleteSession(testToken);

        // Assert
        verify(sessionRepository).deleteByToken(testToken);
    }

    @Test
    void deleteAllUserSessions_success_deletesAllSessions() {
        // Arrange
        doNothing().when(sessionRepository).deleteByUserId(testUserId);

        // Act
        tokenService.deleteAllUserSessions(testUserId);

        // Assert
        verify(sessionRepository).deleteByUserId(testUserId);
    }

    @Test
    void deleteAllUserSessionsExceptCurrent_success_deletesOtherSessions() {
        // Arrange
        String currentToken = UUID.randomUUID().toString();
        doNothing().when(sessionRepository).deleteByUserIdAndTokenNot(testUserId, currentToken);

        // Act
        tokenService.deleteAllUserSessionsExceptCurrent(testUserId, currentToken);

        // Assert
        verify(sessionRepository).deleteByUserIdAndTokenNot(testUserId, currentToken);
    }

    // ===== Verification Token Tests =====

    @Test
    void generateVerificationToken_success_returnsValidToken() {
        // Arrange
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        String token = tokenService.generateVerificationToken(testUserId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(verificationTokenRepository).save(argThat(vToken -> 
            vToken.getUserId().equals(testUserId) &&
            vToken.getToken() != null &&
            vToken.getExpiresAt() != null &&
            vToken.getUsed() == false
        ));
    }

    @Test
    void generateVerificationToken_setsCorrectExpiry() {
        // Arrange
        final LocalDateTime[] capturedExpiresAt = new LocalDateTime[1];
        
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            capturedExpiresAt[0] = token.getExpiresAt();
            token.setId(1L);
            return token;
        });

        // Act
        LocalDateTime beforeGeneration = LocalDateTime.now();
        tokenService.generateVerificationToken(testUserId);
        LocalDateTime afterGeneration = LocalDateTime.now();

        // Assert
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        assertNotNull(capturedExpiresAt[0]);
        
        // Expiry should be 24 hours from generation time (with tolerance)
        LocalDateTime expectedMin = beforeGeneration.plusHours(24).minusSeconds(1);
        LocalDateTime expectedMax = afterGeneration.plusHours(24).plusSeconds(1);
        assertTrue(capturedExpiresAt[0].isAfter(expectedMin) && capturedExpiresAt[0].isBefore(expectedMax),
            "Expected expiresAt to be ~24 hours from now, but was: " + capturedExpiresAt[0]);
    }

    @Test
    void validateVerificationToken_validToken_returnsTokenAndMarksAsUsed() {
        // Arrange
        when(verificationTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testVerificationToken));
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(testVerificationToken);

        // Act
        Long userId = tokenService.validateVerificationToken(testToken);

        // Assert
        assertNotNull(userId);
        assertEquals(testUserId, userId);
        verify(verificationTokenRepository).findByToken(testToken);
        verify(verificationTokenRepository).save(argThat(token -> token.getUsed() == true));
    }

    @Test
    void validateVerificationToken_tokenNotFound_throwsValidationException() {
        // Arrange
        when(verificationTokenRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validateVerificationToken(testToken));
        
        assertEquals("Invalid verification token", exception.getMessage());
        verify(verificationTokenRepository).findByToken(testToken);
        verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    void validateVerificationToken_expiredToken_throwsValidationException() {
        // Arrange
        testVerificationToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(verificationTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testVerificationToken));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validateVerificationToken(testToken));
        
        assertEquals("Verification token has expired", exception.getMessage());
        verify(verificationTokenRepository).findByToken(testToken);
        verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    void validateVerificationToken_alreadyUsed_throwsValidationException() {
        // Arrange
        testVerificationToken.setUsed(true);
        when(verificationTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testVerificationToken));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validateVerificationToken(testToken));
        
        assertEquals("Verification token has already been used", exception.getMessage());
        verify(verificationTokenRepository).findByToken(testToken);
        verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }

    // ===== Password Reset Token Tests =====

    @Test
    void generatePasswordResetToken_success_returnsValidToken() {
        // Arrange
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        String token = tokenService.generatePasswordResetToken(testUserId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(passwordResetTokenRepository).save(argThat(pToken -> 
            pToken.getUserId().equals(testUserId) &&
            pToken.getToken() != null &&
            pToken.getExpiresAt() != null &&
            pToken.getUsed() == false
        ));
    }

    @Test
    void generatePasswordResetToken_setsCorrectExpiry() {
        // Arrange
        final LocalDateTime[] capturedExpiresAt = new LocalDateTime[1];
        
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            capturedExpiresAt[0] = token.getExpiresAt();
            token.setId(1L);
            return token;
        });

        // Act
        LocalDateTime beforeGeneration = LocalDateTime.now();
        tokenService.generatePasswordResetToken(testUserId);
        LocalDateTime afterGeneration = LocalDateTime.now();

        // Assert
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        assertNotNull(capturedExpiresAt[0]);
        
        // Expiry should be 1 hour from generation time (with tolerance)
        LocalDateTime expectedMin = beforeGeneration.plusHours(1).minusSeconds(1);
        LocalDateTime expectedMax = afterGeneration.plusHours(1).plusSeconds(1);
        assertTrue(capturedExpiresAt[0].isAfter(expectedMin) && capturedExpiresAt[0].isBefore(expectedMax),
            "Expected expiresAt to be ~1 hour from now, but was: " + capturedExpiresAt[0]);
    }

    @Test
    void validatePasswordResetToken_validToken_returnsTokenAndMarksAsUsed() {
        // Arrange
        when(passwordResetTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testPasswordResetToken));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(testPasswordResetToken);

        // Act
        Long userId = tokenService.validatePasswordResetToken(testToken);

        // Assert
        assertNotNull(userId);
        assertEquals(testUserId, userId);
        verify(passwordResetTokenRepository).findByToken(testToken);
        verify(passwordResetTokenRepository).save(argThat(token -> token.getUsed() == true));
    }

    @Test
    void validatePasswordResetToken_tokenNotFound_throwsValidationException() {
        // Arrange
        when(passwordResetTokenRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validatePasswordResetToken(testToken));
        
        assertEquals("Invalid password reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken);
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void validatePasswordResetToken_expiredToken_throwsValidationException() {
        // Arrange
        testPasswordResetToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testPasswordResetToken));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validatePasswordResetToken(testToken));
        
        assertEquals("Password reset token has expired", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken);
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void validatePasswordResetToken_alreadyUsed_throwsValidationException() {
        // Arrange
        testPasswordResetToken.setUsed(true);
        when(passwordResetTokenRepository.findByToken(testToken)).thenReturn(Optional.of(testPasswordResetToken));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> tokenService.validatePasswordResetToken(testToken));
        
        assertEquals("Password reset token has already been used", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken);
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    // ===== Token Generation Uniqueness Tests =====

    @Test
    void generatedTokens_shouldBeUnique() {
        // Arrange & Act
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String token1 = tokenService.generateSessionToken(testUserId);
        String token2 = tokenService.generateSessionToken(testUserId);
        String token3 = tokenService.generateSessionToken(testUserId);

        // Assert
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);
    }

    @Test
    void verificationTokens_shouldBeUnique() {
        // Arrange & Act
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String token1 = tokenService.generateVerificationToken(testUserId);
        String token2 = tokenService.generateVerificationToken(testUserId);
        String token3 = tokenService.generateVerificationToken(testUserId);

        // Assert
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);
    }

    @Test
    void passwordResetTokens_shouldBeUnique() {
        // Arrange & Act
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String token1 = tokenService.generatePasswordResetToken(testUserId);
        String token2 = tokenService.generatePasswordResetToken(testUserId);
        String token3 = tokenService.generatePasswordResetToken(testUserId);

        // Assert
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);
    }
}
