package tubes.pbo.be.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.user.model.User;

@Component
public class SecurityContextHelper {

    /**
     * Get the current authenticated user's ID
     * @return User ID
     * @throws UnauthorizedException if user is not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal() == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException();
        }
        
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        
        throw new UnauthorizedException();
    }

    /**
     * Get the current authenticated user
     * @return User entity
     * @throws UnauthorizedException if user is not authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal() == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException();
        }
        
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        
        throw new UnauthorizedException();
    }
}
