package tubes.pbo.be.shared.exception;

/**
 * Exception thrown when configuration is invalid or initialization fails
 */
public class ConfigurationException extends RuntimeException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
