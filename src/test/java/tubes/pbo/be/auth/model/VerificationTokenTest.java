package tubes.pbo.be.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationTokenTest {

    private VerificationToken token;

    @BeforeEach
    void setUp() {
        token = new VerificationToken();
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
        String tokenStr = "test-token";
        Long userId = 10L;
        User user = new User();
        user.setId(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();
        Boolean used = false;

        // Act
        VerificationToken testToken = new VerificationToken(id, tokenStr, userId, user, expiresAt, createdAt, used);

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
        String tokenStr = "test-token";
        Long userId = 10L;
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
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
        VerificationToken token1 = new VerificationToken();
        token1.setId(1L);
        token1.setToken("test-token");

        VerificationToken token2 = new VerificationToken();
        token2.setId(1L);
        token2.setToken("test-token");

        // Assert
        assertThat(token1).isEqualTo(token2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        VerificationToken token1 = new VerificationToken();
        token1.setId(1L);
        token1.setToken("test-token");

        VerificationToken token2 = new VerificationToken();
        token2.setId(1L);
        token2.setToken("test-token");

        // Assert
        assertThat(token1).hasSameHashCodeAs(token2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        token.setId(1L);
        token.setToken("test-token");

        // Act
        String result = token.toString();

        // Assert
        assertThat(result)
            .contains("VerificationToken")
            .contains("id=1")
            .contains("token=test-token");
    }
}
