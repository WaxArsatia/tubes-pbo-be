package tubes.pbo.be.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tubes.pbo.be.auth.model.PasswordResetToken;
import tubes.pbo.be.auth.model.Session;
import tubes.pbo.be.auth.model.VerificationToken;
import tubes.pbo.be.auth.repository.PasswordResetTokenRepository;
import tubes.pbo.be.auth.repository.SessionRepository;
import tubes.pbo.be.auth.repository.VerificationTokenRepository;
import tubes.pbo.be.shared.exception.UnauthorizedException;
import tubes.pbo.be.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final SessionRepository sessionRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.session.expiry-hours:24}")
    private int sessionExpiryHours;

    @Value("${app.token.email-verification-expiry-hours:24}")
    private int emailVerificationExpiryHours;

    @Value("${app.token.password-reset-expiry-hours:1}")
    private int passwordResetExpiryHours;

    public String generateSessionToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(sessionExpiryHours);

        Session session = new Session();
        session.setToken(token);
        session.setUserId(userId);
        session.setExpiresAt(expiresAt);

        sessionRepository.save(session);
        return token;
    }

    public Long validateSessionToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new UnauthorizedException("Session has expired");
        }

        return session.getUserId();
    }

    @Transactional
    public void deleteSession(String token) {
        sessionRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllUserSessions(Long userId) {
        sessionRepository.deleteByUserId(userId);
    }


    @Transactional
    public void deleteAllUserSessionsExceptCurrent(Long userId, String currentToken) {
        sessionRepository.deleteByUserIdAndTokenNot(userId, currentToken);
    }

    public String generateVerificationToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(emailVerificationExpiryHours);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUserId(userId);
        verificationToken.setExpiresAt(expiresAt);
        verificationToken.setUsed(false);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public Long validateVerificationToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid verification token"));

        if (verificationToken.getUsed()) {
            throw new ValidationException("Verification token has already been used");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Verification token has expired");
        }

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        return verificationToken.getUserId();
    }

    public String generatePasswordResetToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(passwordResetExpiryHours);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(userId);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    @Transactional
    public Long validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid password reset token"));

        if (resetToken.getUsed()) {
            throw new ValidationException("Password reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Password reset token has expired");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return resetToken.getUserId();
    }
}
