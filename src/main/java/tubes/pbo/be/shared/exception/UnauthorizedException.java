package tubes.pbo.be.shared.exception;

public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException() {
        super("Authentication token is missing or invalid");
    }
}
