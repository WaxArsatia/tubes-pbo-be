package tubes.pbo.be.user.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void onCreate_shouldSetCreatedAtAndInitializeDefaults() {
        // Act
        user.onCreate();

        // Assert
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(user.getIsVerified()).isFalse();
    }

    @Test
    void onCreate_shouldNotOverrideRoleIfAlreadySet() {
        // Arrange
        user.setRole(User.UserRole.ADMIN);

        // Act
        user.onCreate();

        // Assert
        assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    void onCreate_shouldSetRoleToUserIfNull() {
        // Arrange
        user.setRole(null);

        // Act
        user.onCreate();

        // Assert
        assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
    }

    @Test
    void onCreate_shouldNotOverrideIsVerifiedIfAlreadySet() {
        // Arrange
        user.setIsVerified(true);

        // Act
        user.onCreate();

        // Assert
        assertThat(user.getIsVerified()).isTrue();
    }

    @Test
    void onCreate_shouldSetIsVerifiedToFalseIfNull() {
        // Arrange
        user.setIsVerified(null);

        // Act
        user.onCreate();

        // Assert
        assertThat(user.getIsVerified()).isFalse();
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Long id = 1L;
        String email = "test@example.com";
        String password = "password123";
        String name = "Test User";
        User.UserRole role = User.UserRole.ADMIN;
        Boolean isVerified = true;
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        User testUser = new User(id, email, password, name, role, isVerified, createdAt);

        // Assert
        assertThat(testUser.getId()).isEqualTo(id);
        assertThat(testUser.getEmail()).isEqualTo(email);
        assertThat(testUser.getPassword()).isEqualTo(password);
        assertThat(testUser.getName()).isEqualTo(name);
        assertThat(testUser.getRole()).isEqualTo(role);
        assertThat(testUser.getIsVerified()).isEqualTo(isVerified);
        assertThat(testUser.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        String email = "test@example.com";
        String password = "password123";
        String name = "Test User";
        User.UserRole role = User.UserRole.ADMIN;
        Boolean isVerified = true;
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);
        user.setIsVerified(isVerified);
        user.setCreatedAt(createdAt);

        // Assert
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.getIsVerified()).isEqualTo(isVerified);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void userRoleEnum_shouldHaveCorrectValues() {
        // Assert
        assertThat(User.UserRole.values()).containsExactly(User.UserRole.USER, User.UserRole.ADMIN);
        assertThat(User.UserRole.valueOf("USER")).isEqualTo(User.UserRole.USER);
        assertThat(User.UserRole.valueOf("ADMIN")).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    void equals_shouldWorkWithLombok() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setId(1L);
        user2.setEmail("test@example.com");

        // Assert
        assertThat(user1).isEqualTo(user2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setId(1L);
        user2.setEmail("test@example.com");

        // Assert
        assertThat(user1).hasSameHashCodeAs(user2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");

        // Act
        String result = user.toString();

        // Assert
        assertThat(result)
            .contains("User")
            .contains("id=1")
            .contains("email=test@example.com")
            .contains("name=Test User");
    }
}
