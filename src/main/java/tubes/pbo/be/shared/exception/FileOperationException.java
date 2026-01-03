package tubes.pbo.be.shared.exception;

/**
 * Exception thrown when a file operation fails
 */
public class FileOperationException extends RuntimeException {
    
    public FileOperationException(String message) {
        super(message);
    }
    
    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
