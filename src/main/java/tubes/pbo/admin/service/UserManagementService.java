package tubes.pbo.be.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tubes.pbo.be.admin.dto.CreateUserRequest;
import tubes.pbo.be.admin.dto.UpdateUserRequest;
import tubes.pbo.be.admin.dto.UserDetailResponse;
import tubes.pbo.be.auth.repository.PasswordResetTokenRepository;
import tubes.pbo.be.auth.repository.SessionRepository;
import tubes.pbo.be.auth.repository.VerificationTokenRepository;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.summary.repository.SummaryRepository;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.repository.UserRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    
    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";
    
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final SessionRepository sessionRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * List all users with optional search filter
     * Search filters by email OR name (case-insensitive)
     */
    public Page<UserDetailResponse> listUsers(String search, Pageable pageable) {
        Page<User> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.searchUsers(search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        
        List<UserDetailResponse> responses = users.getContent().stream()
                .map(this::convertToUserDetailResponse)
                .toList();
        
        return new PageImpl<>(responses, pageable, users.getTotalElements());
    }
    
    /**
     * Get detailed user information including total summaries count
     */
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MSG + userId));
        
        return convertToUserDetailResponse(user);
    }
    
    /**
     * Create a new user (auto-verified)
     * Admin-created users are automatically verified
     */
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(User.UserRole.valueOf(request.getRole().toUpperCase()));
        user.setIsVerified(true); // Admin-created users are auto-verified
        
        user = userRepository.save(user);
        
        return convertToUserDetailResponse(user);
    }
    
    /**
     * Update user information (except password)
     * Can update: email, name, role, isVerified
     */
    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MSG + userId));
        
        // Check if email is being changed to an existing email (but not the same user)
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }
        
        // Update user fields
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setRole(User.UserRole.valueOf(request.getRole().toUpperCase()));
        user.setIsVerified(request.getIsVerified());
        
        user = userRepository.save(user);
        
        return convertToUserDetailResponse(user);
    }
    
    /**
     * Delete user and cascade delete all related data
     * Cannot delete self
     */
    @Transactional
    public void deleteUser(Long currentUserId, Long userIdToDelete) {
        // Prevent self-delete
        if (currentUserId.equals(userIdToDelete)) {
            throw new ForbiddenException("Cannot delete your own account");
        }
        
        User user = userRepository.findById(userIdToDelete)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MSG + userIdToDelete));
        
        // Delete user's PDF files from filesystem
        deleteUserFiles(userIdToDelete);
        
        // Delete related tokens (cascade will handle sessions via JPA)
        sessionRepository.deleteByUserId(userIdToDelete);
        verificationTokenRepository.deleteByUserId(userIdToDelete);
        passwordResetTokenRepository.deleteByUserId(userIdToDelete);
        
        // Delete summaries (cascade should handle quizzes and questions via JPA relationships)
        summaryRepository.deleteAll(summaryRepository.findByUserId(userIdToDelete, Pageable.unpaged()).getContent());
        
        // Finally delete the user
        userRepository.delete(user);
    }
    
    /**
     * Helper method to convert User entity to UserDetailResponse
     */
    private UserDetailResponse convertToUserDetailResponse(User user) {
        long summaryCount = summaryRepository.countByUserId(user.getId());
        
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .totalSummaries(summaryCount)
                .build();
    }
    
    /**
     * Delete all PDF files for a user from filesystem
     */
    private void deleteUserFiles(Long userId) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/pdfs/" + userId;
            Path userPath = Paths.get(uploadDir);
            
            if (Files.exists(userPath)) {
                try (var stream = Files.walk(userPath)) {
                    stream.map(Path::toFile)
                            .forEach(File::delete);
                }
                Files.deleteIfExists(userPath);
            }
        } catch (Exception _) {
            // Log error but don't fail the deletion
            // Note: Consider adding SLF4J logger for production
            // For now, we'll suppress the error as it's a non-critical cleanup
        }
    }
}
