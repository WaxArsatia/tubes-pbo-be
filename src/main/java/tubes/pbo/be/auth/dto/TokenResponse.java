package tubes.pbo.be.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication token response")
public class TokenResponse {

    @Schema(description = "Session token (UUID format)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @Schema(description = "User information")
    private UserInfo user;
}
