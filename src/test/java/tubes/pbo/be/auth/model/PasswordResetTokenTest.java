package tubes.pbo.be.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        token = new PasswordResetToken();
    }

    @Test
    void onCreate_shouldSetCreatedAtAndInitializeUsedFlag() {
        // Act
        token.onCreate();

        // Assert
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(token.getUsed()).isFalse();
    }

    @Test
    void onCreate_shouldNotOverrideUsedIfAlreadySet() {
        // Arrange
        token.setUsed(true);

        // Act
        token.onCreate();

        // Assert
        assertThat(token.getUsed()).isTrue();
    }

    @Test
    void onCreate_shouldSetUsedToFalseIfNull() {
        // Arrange
        token.setUsed(null);

        // Act
        token.onCreate();

        // Assert
        assertThat(token.getUsed()).isFalse();
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Long id = 1L;
        String tokenStr = "reset-token";
        Long userId = 10L;
        User user = new User();
        user.setId(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now();
        Boolean used = false;

        // Act
        PasswordResetToken testToken = new PasswordResetToken(id, tokenStr, userId, user, expiresAt, createdAt, used);

        // Assert
        assertThat(testToken.getId()).isEqualTo(id);
        assertThat(testToken.getToken()).isEqualTo(tokenStr);
        assertThat(testToken.getUserId()).isEqualTo(userId);
        assertThat(testToken.getUser()).isEqualTo(user);
        assertThat(testToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(testToken.getCreatedAt()).isEqualTo(createdAt);
        assertThat(testToken.getUsed()).isEqualTo(used);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        String tokenStr = "reset-token";
        Long userId = 10L;
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now();
        Boolean used = false;

        // Act
        token.setId(id);
        token.setToken(tokenStr);
        token.setUserId(userId);
        token.setUser(user);
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(createdAt);
        token.setUsed(used);

        // Assert
        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getToken()).isEqualTo(tokenStr);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        assertThat(token.getUsed()).isEqualTo(used);
    }

    @Test
    void equals_shouldWorkWithLombok() {
        // Arrange
        PasswordResetToken token1 = new PasswordResetToken();
        token1.setId(1L);
        token1.setToken("reset-token");

        PasswordResetToken token2 = new PasswordResetToken();
        token2.setId(1L);
        token2.setToken("reset-token");

        // Assert
        assertThat(token1).isEqualTo(token2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        PasswordResetToken token1 = new PasswordResetToken();
        token1.setId(1L);
        token1.setToken("reset-token");

        PasswordResetToken token2 = new PasswordResetToken();
        token2.setId(1L);
        token2.setToken("reset-token");

        // Assert
        assertThat(token1).hasSameHashCodeAs(token2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        token.setId(1L);
        token.setToken("reset-token");

        // Act
        String result = token.toString();

        // Assert
        assertThat(result)
            .contains("PasswordResetToken")
            .contains("id=1")
            .contains("token=reset-token");
    }
}
