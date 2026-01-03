package tubes.pbo.be.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tubes.pbo.be.admin.dto.*;
import tubes.pbo.be.admin.service.AdminService;
import tubes.pbo.be.admin.service.DashboardService;
import tubes.pbo.be.admin.service.UserManagementService;
import tubes.pbo.be.shared.dto.ApiResponse;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.security.SecurityContextHelper;
import tubes.pbo.be.user.model.User;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management endpoints for user management and monitoring")
public class AdminController {
    
    private final UserManagementService userManagementService;
    private final DashboardService dashboardService;
    private final AdminService adminService;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Check if current user is admin, throw exception if not
     */
    private void requireAdminRole() {
        User currentUser = securityContextHelper.getCurrentUser();
        if (!currentUser.getRole().equals(User.UserRole.ADMIN)) {
            throw new ForbiddenException("Admin access required");
        }
    }
    
    // ========== User Management Endpoints ==========
    
    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Get paginated list of users with optional search filter. Admin only.")
    public PageResponse<UserDetailResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireAdminRole();
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDetailResponse> users = userManagementService.listUsers(search, pageable);
        
        return new PageResponse<>(users);
    }
    
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user detail", description = "Get detailed information about a specific user. Admin only.")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        requireAdminRole();
        
        UserDetailResponse user = userManagementService.getUserDetail(id);
        return new ApiResponse<>("User retrieved successfully", user);
    }
    
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user", description = "Create a new user (auto-verified). Admin only.")
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        requireAdminRole();
        
        UserDetailResponse user = userManagementService.createUser(request);
        return new ApiResponse<>("User created successfully", user);
    }
    
    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Update user information (except password). Admin only.")
    public ApiResponse<UserDetailResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        requireAdminRole();
        
        UserDetailResponse user = userManagementService.updateUser(id, request);
        return new ApiResponse<>("User updated successfully", user);
    }
    
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Delete a user and cascade delete all related data. Cannot delete self. Admin only.")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        requireAdminRole();
        
        Long currentUserId = securityContextHelper.getCurrentUserId();
        userManagementService.deleteUser(currentUserId, id);
        
        return new ApiResponse<>("User deleted successfully", null);
    }
    
    // ========== Monitoring Endpoints ==========
    
    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", description = "Get comprehensive dashboard statistics for monitoring. Admin only.")
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        requireAdminRole();
        
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return new ApiResponse<>("Dashboard statistics retrieved successfully", stats);
    }
    
    @GetMapping("/activity")
    @Operation(summary = "Get user activity log", description = "Get paginated list of all summaries with user information. Admin only.")
    public PageResponse<ActivityLogResponse> getUserActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        requireAdminRole();
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogResponse> activities = adminService.getUserActivity(pageable);
        
        return new PageResponse<>(activities);
    }
}
