package tubes.pbo.be.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.user.model.User;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityContextHelperTest {

    private SecurityContextHelper securityContextHelper;

    @BeforeEach
    void setUp() {
        securityContextHelper = new SecurityContextHelper();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_withValidAuthentication_shouldReturnUserId() {
        // Arrange
        User user = new User();
        user.setId(123L);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Long result = securityContextHelper.getCurrentUserId();

        // Assert
        assertThat(result).isEqualTo(123L);
    }

    @Test
    void getCurrentUserId_withNullAuthentication_shouldThrowUnauthorizedException() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
            .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAuthenticationScenarios")
    void getCurrentUserId_withInvalidAuthentication_shouldThrowUnauthorizedException(Authentication auth, String scenario) {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
            .isInstanceOf(UnauthorizedException.class);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideInvalidAuthenticationScenarios() {
        Authentication unauthenticated = mock(Authentication.class);
        when(unauthenticated.isAuthenticated()).thenReturn(false);

        Authentication nullPrincipal = mock(Authentication.class);
        when(nullPrincipal.isAuthenticated()).thenReturn(true);
        when(nullPrincipal.getPrincipal()).thenReturn(null);

        Authentication anonymousUser = mock(Authentication.class);
        when(anonymousUser.isAuthenticated()).thenReturn(true);
        when(anonymousUser.getPrincipal()).thenReturn("anonymousUser");

        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(unauthenticated, "unauthenticated"),
            org.junit.jupiter.params.provider.Arguments.of(nullPrincipal, "null principal"),
            org.junit.jupiter.params.provider.Arguments.of(anonymousUser, "anonymous user")
        );
    }

    @Test
    void getCurrentUserId_withNonUserPrincipal_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("someString");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withValidAuthentication_shouldReturnUser() {
        // Arrange
        User user = new User();
        user.setId(456L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        User result = securityContextHelper.getCurrentUser();

        // Assert
        assertThat(result).isEqualTo(user);
        assertThat(result.getId()).isEqualTo(456L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getCurrentUser_withNullAuthentication_shouldThrowUnauthorizedException() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withUnauthenticated_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withNullPrincipal_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withAnonymousUser_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withNonUserPrincipal_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(123);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_withEmptySecurityContext_shouldThrowUnauthorizedException() {
        // Arrange
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(emptyContext);

        // Act & Assert
        assertThatThrownBy(() -> securityContextHelper.getCurrentUser())
            .isInstanceOf(UnauthorizedException.class);
    }
}
