package tubes.pbo.be.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user information (password update not supported)")
public class UpdateUserRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    @Size(max = 255, message = "Email must be less than 255 characters")
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "User's full name", example = "John Doe")
    private String name;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Role must be either USER or ADMIN")
    @Schema(description = "User role", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;
    
    @NotNull(message = "isVerified is required")
    @Schema(description = "Whether user's email is verified", example = "true")
    private Boolean isVerified;
}
