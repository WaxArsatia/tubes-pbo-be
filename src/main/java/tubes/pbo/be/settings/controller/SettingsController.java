package tubes.pbo.be.settings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tubes.pbo.be.settings.dto.ChangePasswordRequest;
import tubes.pbo.be.settings.dto.ProfileResponse;
import tubes.pbo.be.settings.dto.UpdateProfileRequest;
import tubes.pbo.be.settings.service.SettingsService;
import tubes.pbo.be.shared.dto.ApiResponse;
import tubes.pbo.be.shared.security.SecurityContextHelper;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "User settings and profile management")
public class SettingsController {

    private final SettingsService settingsService;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping
    @Operation(
            summary = "Get profile",
            description = "Retrieve current user's profile information",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        Long userId = securityContextHelper.getCurrentUserId();
        ProfileResponse profile = settingsService.getProfile(userId);
        return ResponseEntity.ok(new ApiResponse<>("Profile retrieved successfully", profile));
    }

    @PutMapping
    @Operation(
            summary = "Update profile",
            description = "Update user's profile name",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityContextHelper.getCurrentUserId();
        ProfileResponse updatedProfile = settingsService.updateProfile(userId, request);
        
        // Return only the updated name field as per spec
        Map<String, String> response = Map.of("name", updatedProfile.getName());
        return ResponseEntity.ok(new ApiResponse<>("Name updated successfully", response));
    }

    @PutMapping("/password")
    @Operation(
            summary = "Change password",
            description = "Change user's password. Invalidates all other sessions except current one.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = securityContextHelper.getCurrentUserId();
        
        // Extract token from "Bearer {token}"
        String currentToken = authHeader.replace("Bearer ", "");
        
        settingsService.changePassword(userId, currentToken, request);
        return ResponseEntity.ok(new ApiResponse<>("Password changed successfully", null));
    }
}
