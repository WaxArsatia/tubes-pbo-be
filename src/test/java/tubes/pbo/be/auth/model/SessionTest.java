package tubes.pbo.be.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        // Act
        session.onCreate();

        // Assert
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Long id = 1L;
        String token = "session-token";
        Long userId = 10L;
        User user = new User();
        user.setId(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        Session testSession = new Session(id, token, userId, user, expiresAt, createdAt);

        // Assert
        assertThat(testSession.getId()).isEqualTo(id);
        assertThat(testSession.getToken()).isEqualTo(token);
        assertThat(testSession.getUserId()).isEqualTo(userId);
        assertThat(testSession.getUser()).isEqualTo(user);
        assertThat(testSession.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(testSession.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        String token = "session-token";
        Long userId = 10L;
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        session.setId(id);
        session.setToken(token);
        session.setUserId(userId);
        session.setUser(user);
        session.setExpiresAt(expiresAt);
        session.setCreatedAt(createdAt);

        // Assert
        assertThat(session.getId()).isEqualTo(id);
        assertThat(session.getToken()).isEqualTo(token);
        assertThat(session.getUserId()).isEqualTo(userId);
        assertThat(session.getUser()).isEqualTo(user);
        assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void equals_shouldWorkWithLombok() {
        // Arrange
        Session session1 = new Session();
        session1.setId(1L);
        session1.setToken("session-token");

        Session session2 = new Session();
        session2.setId(1L);
        session2.setToken("session-token");

        // Assert
        assertThat(session1).isEqualTo(session2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        Session session1 = new Session();
        session1.setId(1L);
        session1.setToken("session-token");

        Session session2 = new Session();
        session2.setId(1L);
        session2.setToken("session-token");

        // Assert
        assertThat(session1).hasSameHashCodeAs(session2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        session.setId(1L);
        session.setToken("session-token");

        // Act
        String result = session.toString();

        // Assert
        assertThat(result)
            .contains("Session")
            .contains("id=1")
            .contains("token=session-token");
    }
}
