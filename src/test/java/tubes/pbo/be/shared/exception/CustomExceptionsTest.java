package tubes.pbo.be.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionsTest {

    @Test
    void unauthorizedException_withoutMessage_shouldUseDefaultMessage() {
        // Act
        UnauthorizedException exception = new UnauthorizedException();

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Authentication token is missing or invalid");
    }

    @Test
    void unauthorizedException_withCustomMessage_shouldUseCustomMessage() {
        // Act
        UnauthorizedException exception = new UnauthorizedException("Invalid token");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Invalid token");
    }

    @Test
    void forbiddenException_withoutMessage_shouldUseDefaultMessage() {
        // Act
        ForbiddenException exception = new ForbiddenException();

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void forbiddenException_withCustomMessage_shouldUseCustomMessage() {
        // Act
        ForbiddenException exception = new ForbiddenException("Admin access required");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Admin access required");
    }

    @Test
    void fileOperationException_withMessage_shouldSetMessage() {
        // Act
        FileOperationException exception = new FileOperationException("File upload failed");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("File upload failed");
    }

    @Test
    void fileOperationException_withMessageAndCause_shouldSetBoth() {
        // Arrange
        Throwable cause = new RuntimeException("IO error");

        // Act
        FileOperationException exception = new FileOperationException("File upload failed", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("File upload failed");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("IO error");
    }

    @Test
    void configurationException_withMessage_shouldSetMessage() {
        // Act
        ConfigurationException exception = new ConfigurationException("Invalid configuration");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Invalid configuration");
    }

    @Test
    void aiServiceException_withMessage_shouldSetMessage() {
        // Act
        AiServiceException exception = new AiServiceException("AI API failed");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("AI API failed");
    }

    @Test
    void aiServiceException_withMessageAndCause_shouldSetBoth() {
        // Arrange
        Throwable cause = new RuntimeException("Network error");

        // Act
        AiServiceException exception = new AiServiceException("AI API failed", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("AI API failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void validationException_withMessage_shouldSetMessage() {
        // Act
        ValidationException exception = new ValidationException("Invalid input");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void resourceNotFoundException_withMessage_shouldSetMessage() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("User not found");
    }

    @Test
    void allExceptions_shouldBeRuntimeExceptions() {
        // Assert
        assertThat(new UnauthorizedException()).isInstanceOf(RuntimeException.class);
        assertThat(new ForbiddenException()).isInstanceOf(RuntimeException.class);
        assertThat(new FileOperationException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new ConfigurationException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new AiServiceException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new ValidationException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new ResourceNotFoundException("test")).isInstanceOf(RuntimeException.class);
    }
}
