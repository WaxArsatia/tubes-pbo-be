package tubes.pbo.be.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import tubes.pbo.be.user.model.User.UserRole;
import tubes.pbo.be.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private User testUser1;
    private User testUser2;
    private User adminUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setEmail("user1@example.com");
        testUser1.setPassword("hashedPassword");
        testUser1.setName("Test User 1");
        testUser1.setRole(UserRole.USER);
        testUser1.setIsVerified(true);
        testUser1.setCreatedAt(LocalDateTime.now());

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setEmail("user2@example.com");
        testUser2.setPassword("hashedPassword");
        testUser2.setName("Test User 2");
        testUser2.setRole(UserRole.USER);
        testUser2.setIsVerified(false);
        testUser2.setCreatedAt(LocalDateTime.now());

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("hashedPassword");
        adminUser.setName("Admin User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsVerified(true);
        adminUser.setCreatedAt(LocalDateTime.now());

        // Setup create user request
        createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setPassword("password123");
        createUserRequest.setName("New User");
        createUserRequest.setRole("USER");

        // Setup update user request
        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("updated@example.com");
        updateUserRequest.setName("Updated Name");
        updateUserRequest.setRole("USER");
        updateUserRequest.setIsVerified(true);

        // Setup pageable
        pageable = PageRequest.of(0, 20);
    }

    // ===== listUsers Tests =====

    @Test
    void listUsers_withoutSearch_returnsAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser1, testUser2, adminUser);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);
        when(summaryRepository.countByUserId(2L)).thenReturn(3L);
        when(summaryRepository.countByUserId(3L)).thenReturn(10L);

        // Act
        Page<UserDetailResponse> result = userManagementService.listUsers(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).searchUsers(anyString(), any());
    }

    @Test
    void listUsers_withEmptySearch_returnsAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser1, testUser2);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(summaryRepository.countByUserId(anyLong())).thenReturn(0L);

        // Act
        Page<UserDetailResponse> result = userManagementService.listUsers("  ", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).searchUsers(anyString(), any());
    }

    @Test
    void listUsers_withSearch_returnsFilteredUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser1);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        String searchTerm = "user1";
        
        when(userRepository.searchUsers(searchTerm, pageable)).thenReturn(userPage);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        Page<UserDetailResponse> result = userManagementService.listUsers(searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("user1@example.com", result.getContent().get(0).getEmail());
        verify(userRepository).searchUsers(searchTerm, pageable);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listUsers_withSearchTrimming_trimsWhitespace() {
        // Arrange
        List<User> users = Arrays.asList(testUser1);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        
        when(userRepository.searchUsers("test", pageable)).thenReturn(userPage);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        Page<UserDetailResponse> result = userManagementService.listUsers("  test  ", pageable);

        // Assert
        assertNotNull(result);
        verify(userRepository).searchUsers("test", pageable);
    }

    @Test
    void listUsers_userDetailResponse_includesSummaryCount() {
        // Arrange
        List<User> users = Arrays.asList(testUser1);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(summaryRepository.countByUserId(1L)).thenReturn(7L);

        // Act
        Page<UserDetailResponse> result = userManagementService.listUsers(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        UserDetailResponse response = result.getContent().get(0);
        assertEquals(7L, response.getTotalSummaries());
        assertEquals(testUser1.getId(), response.getId());
        assertEquals(testUser1.getEmail(), response.getEmail());
        assertEquals(testUser1.getName(), response.getName());
        assertEquals(testUser1.getRole().name(), response.getRole());
        assertEquals(testUser1.getIsVerified(), response.getIsVerified());
    }

    // ===== getUserDetail Tests =====

    @Test
    void getUserDetail_validUserId_returnsUserDetailResponse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        UserDetailResponse result = userManagementService.getUserDetail(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user1@example.com", result.getEmail());
        assertEquals("Test User 1", result.getName());
        assertEquals("USER", result.getRole());
        assertTrue(result.getIsVerified());
        assertEquals(5L, result.getTotalSummaries());
        verify(userRepository).findById(1L);
        verify(summaryRepository).countByUserId(1L);
    }

    @Test
    void getUserDetail_invalidUserId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userManagementService.getUserDetail(999L)
        );
        
        assertTrue(exception.getMessage().contains("User not found"));
        assertTrue(exception.getMessage().contains("999"));
        verify(userRepository).findById(999L);
        verify(summaryRepository, never()).countByUserId(anyLong());
    }

    // ===== createUser Tests =====

    @Test
    void createUser_validRequest_createsUserWithAutoVerify() {
        // Arrange
        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setEmail(createUserRequest.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setName(createUserRequest.getName());
        savedUser.setRole(UserRole.USER);
        savedUser.setIsVerified(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(summaryRepository.countByUserId(4L)).thenReturn(0L);

        // Act
        UserDetailResponse result = userManagementService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("New User", result.getName());
        assertEquals("USER", result.getRole());
        assertTrue(result.getIsVerified()); // Auto-verified
        assertEquals(0L, result.getTotalSummaries());
        
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(passwordEncoder).encode(createUserRequest.getPassword());
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals(createUserRequest.getEmail()) &&
            user.getName().equals(createUserRequest.getName()) &&
            user.getRole().equals(UserRole.USER) &&
            user.getIsVerified() == true
        ));
    }

    @Test
    void createUser_adminRole_createsAdminUser() {
        // Arrange
        createUserRequest.setRole("ADMIN");
        
        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setEmail(createUserRequest.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setName(createUserRequest.getName());
        savedUser.setRole(UserRole.ADMIN);
        savedUser.setIsVerified(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(summaryRepository.countByUserId(4L)).thenReturn(0L);

        // Act
        UserDetailResponse result = userManagementService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        assertTrue(result.getIsVerified());
        
        verify(userRepository).save(argThat(user -> user.getRole().equals(UserRole.ADMIN)));
    }

    @Test
    void createUser_duplicateEmail_throwsValidationException() {
        // Arrange
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> userManagementService.createUser(createUserRequest)
        );
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_lowercaseRole_convertsToUppercase() {
        // Arrange
        createUserRequest.setRole("user");
        
        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setEmail(createUserRequest.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setName(createUserRequest.getName());
        savedUser.setRole(UserRole.USER);
        savedUser.setIsVerified(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(summaryRepository.countByUserId(4L)).thenReturn(0L);

        // Act
        UserDetailResponse result = userManagementService.createUser(createUserRequest);

        // Assert
        assertEquals("USER", result.getRole());
    }

    // ===== updateUser Tests =====

    @Test
    void updateUser_validRequest_updatesUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.existsByEmail(updateUserRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser1);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        UserDetailResponse result = userManagementService.updateUser(1L, updateUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("updated@example.com", testUser1.getEmail());
        assertEquals("Updated Name", testUser1.getName());
        assertEquals(UserRole.USER, testUser1.getRole());
        assertTrue(testUser1.getIsVerified());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser1);
    }

    @Test
    void updateUser_sameEmail_allowsUpdate() {
        // Arrange
        updateUserRequest.setEmail(testUser1.getEmail()); // Same email
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.save(any(User.class))).thenReturn(testUser1);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        UserDetailResponse result = userManagementService.updateUser(1L, updateUserRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(testUser1);
    }

    @Test
    void updateUser_duplicateEmail_throwsValidationException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.existsByEmail(updateUserRequest.getEmail())).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> userManagementService.updateUser(1L, updateUserRequest)
        );
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_changeRole_updatesRole() {
        // Arrange
        updateUserRequest.setEmail(testUser1.getEmail());
        updateUserRequest.setRole("ADMIN");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.save(any(User.class))).thenReturn(testUser1);
        when(summaryRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        userManagementService.updateUser(1L, updateUserRequest);

        // Assert
        assertEquals(UserRole.ADMIN, testUser1.getRole());
        verify(userRepository).save(testUser1);
    }

    @Test
    void updateUser_changeVerificationStatus_updatesIsVerified() {
        // Arrange
        testUser2.setIsVerified(false);
        updateUserRequest.setEmail(testUser2.getEmail());
        updateUserRequest.setIsVerified(true);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(userRepository.save(any(User.class))).thenReturn(testUser2);
        when(summaryRepository.countByUserId(2L)).thenReturn(3L);

        // Act
        userManagementService.updateUser(2L, updateUserRequest);

        // Assert
        assertTrue(testUser2.getIsVerified());
        verify(userRepository).save(testUser2);
    }

    @Test
    void updateUser_invalidUserId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userManagementService.updateUser(999L, updateUserRequest)
        );
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    // ===== deleteUser Tests =====

    @Test
    void deleteUser_validRequest_deletesUserAndRelatedData() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(summaryRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));
        
        doNothing().when(sessionRepository).deleteByUserId(1L);
        doNothing().when(verificationTokenRepository).deleteByUserId(1L);
        doNothing().when(passwordResetTokenRepository).deleteByUserId(1L);
        doNothing().when(summaryRepository).deleteAll(anyList());
        doNothing().when(userRepository).delete(testUser1);

        // Act
        userManagementService.deleteUser(3L, 1L); // Admin (3) deleting User (1)

        // Assert
        verify(sessionRepository).deleteByUserId(1L);
        verify(verificationTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(summaryRepository).findByUserId(eq(1L), any(Pageable.class));
        verify(summaryRepository).deleteAll(anyList());
        verify(userRepository).delete(testUser1);
    }

    @Test
    void deleteUser_selfDelete_throwsForbiddenException() {
        // Arrange - Admin trying to delete their own account
        Long adminId = 3L;

        // Act & Assert
        ForbiddenException exception = assertThrows(
            ForbiddenException.class,
            () -> userManagementService.deleteUser(adminId, adminId)
        );
        
        assertEquals("Cannot delete your own account", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_invalidUserId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userManagementService.deleteUser(3L, 999L)
        );
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findById(999L);
        verify(sessionRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_cascadeDeleteOrder_correctOrder() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(summaryRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act
        userManagementService.deleteUser(3L, 1L);

        // Assert - Verify order of deletions
        var inOrder = inOrder(sessionRepository, verificationTokenRepository, 
                              passwordResetTokenRepository, summaryRepository, userRepository);
        
        inOrder.verify(sessionRepository).deleteByUserId(1L);
        inOrder.verify(verificationTokenRepository).deleteByUserId(1L);
        inOrder.verify(passwordResetTokenRepository).deleteByUserId(1L);
        inOrder.verify(summaryRepository).deleteAll(anyList());
        inOrder.verify(userRepository).delete(testUser1);
    }
}
