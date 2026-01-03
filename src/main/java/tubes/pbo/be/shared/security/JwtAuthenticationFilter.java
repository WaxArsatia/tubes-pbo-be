package tubes.pbo.be.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tubes.pbo.be.auth.service.TokenService;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.repository.UserRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authenticateWithToken(request, token);
            }
        } catch (Exception _) {
            // Any error - continue without authentication
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void authenticateWithToken(HttpServletRequest request, String token) {
        try {
            // Validate token and get user ID
            Long userId = tokenService.validateSessionToken(token);
            
            // Get user entity
            User user = userRepository.findById(userId).orElse(null);
            
            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(user, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception _) {
            // Invalid token - continue without authentication
            // Let SecurityConfig handle 401 for protected endpoints
        }
    }
}
