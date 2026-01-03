package tubes.pbo.be.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tubes.pbo.be.auth.dto.*;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Transactional
    public Long register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new ValidationException("Email already exists");
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(User.UserRole.USER);
        user.setIsVerified(false);

        user = userRepository.save(user);

        // Generate verification token and send email
        String verificationToken = tokenService.generateVerificationToken(user.getId());
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return user.getId();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        // Find user by email (case-insensitive)
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ValidationException("Invalid email or password");
        }

        // Check if email is verified
        if (!user.getIsVerified()) {
            throw new ForbiddenException("Please verify your email before logging in");
        }

        // Generate session token
        String sessionToken = tokenService.generateSessionToken(user.getId());

        // Prepare response
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setName(user.getName());
        userInfo.setRole(user.getRole().name());

        TokenResponse response = new TokenResponse();
        response.setToken(sessionToken);
        response.setUser(userInfo);

        return response;
    }

    @Transactional
    public void verifyEmail(String token) {
        Long userId = tokenService.validateVerificationToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Always return success for security (don't reveal if email exists)
        // But only send email if user exists
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String resetToken = tokenService.generatePasswordResetToken(user.getId());
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Long userId = tokenService.validatePasswordResetToken(request.getToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all existing sessions
        tokenService.deleteAllUserSessions(userId);
    }

    @Transactional
    public void logout(String token) {
        tokenService.deleteSession(token);
    }
}
