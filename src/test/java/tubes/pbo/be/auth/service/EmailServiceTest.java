package tubes.pbo.be.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@example.com";
    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test-token-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("sendVerificationEmail - Success")
    void sendVerificationEmail_success() {
        // Act
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertArrayEquals(new String[]{TEST_EMAIL}, sentMessage.getTo());
        assertEquals("Verify Your Email - Tubes PBO", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(TEST_TOKEN));
        assertTrue(sentMessage.getText().contains(FRONTEND_URL + "/verify?token=" + TEST_TOKEN));
    }

    @Test
    @DisplayName("sendVerificationEmail - Contains Verification Link")
    void sendVerificationEmail_containsVerificationLink() {
        // Act
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String expectedLink = FRONTEND_URL + "/verify?token=" + TEST_TOKEN;
        assertTrue(sentMessage.getText().contains(expectedLink),
                "Email should contain verification link: " + expectedLink);
    }

    @Test
    @DisplayName("sendVerificationEmail - Contains Welcome Message")
    void sendVerificationEmail_containsWelcomeMessage() {
        // Act
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Welcome to Tubes PBO"),
                "Email should contain welcome message");
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Success")
    void sendPasswordResetEmail_success() {
        // Act
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertArrayEquals(new String[]{TEST_EMAIL}, sentMessage.getTo());
        assertEquals("Password Reset Request - Tubes PBO", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(TEST_TOKEN));
        assertTrue(sentMessage.getText().contains(FRONTEND_URL + "/reset-password?token=" + TEST_TOKEN));
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Contains Reset Link")
    void sendPasswordResetEmail_containsResetLink() {
        // Act
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String expectedLink = FRONTEND_URL + "/reset-password?token=" + TEST_TOKEN;
        assertTrue(sentMessage.getText().contains(expectedLink),
                "Email should contain password reset link: " + expectedLink);
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Contains Expiry Information")
    void sendPasswordResetEmail_containsExpiryInformation() {
        // Act
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("1 hour") || sentMessage.getText().contains("expire"),
                "Email should mention token expiry");
    }

    @Test
    @DisplayName("sendVerificationEmail - MailSender Throws Exception - Does Not Propagate Exception")
    void sendVerificationEmail_mailSenderThrowsException_doesNotPropagateException() {
        // Arrange
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act - should not throw exception
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert - verify email sending was attempted
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail - MailSender Throws Exception - Does Not Propagate Exception")
    void sendPasswordResetEmail_mailSenderThrowsException_doesNotPropagateException() {
        // Arrange
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act - should not throw exception
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert - verify email sending was attempted
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendVerificationEmail - With Special Characters in Email")
    void sendVerificationEmail_withSpecialCharactersInEmail() {
        // Arrange
        String specialEmail = "test+alias@example.com";

        // Act
        emailService.sendVerificationEmail(specialEmail, TEST_TOKEN);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertArrayEquals(new String[]{specialEmail}, sentMessage.getTo());
    }

    @Test
    @DisplayName("sendPasswordResetEmail - With Special Characters in Token")
    void sendPasswordResetEmail_withSpecialCharactersInToken() {
        // Arrange
        String specialToken = "test-token-with-special-chars-123-abc-XYZ";

        // Act
        emailService.sendPasswordResetEmail(TEST_EMAIL, specialToken);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains(specialToken));
    }
}
