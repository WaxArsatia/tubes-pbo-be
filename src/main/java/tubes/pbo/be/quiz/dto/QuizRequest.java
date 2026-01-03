package tubes.pbo.be.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate a quiz from a summary")
public class QuizRequest {
    
    @NotNull(message = "Summary ID is required")
    @Schema(description = "ID of the summary to generate quiz from", example = "1")
    private Long summaryId;
    
    @NotNull(message = "Difficulty is required")
    @Pattern(regexp = "(?i)easy|medium|hard", message = "Difficulty must be easy, medium, or hard")
    @Schema(description = "Quiz difficulty level", example = "medium", allowableValues = {"easy", "medium", "hard"})
    private String difficulty;
    
    @NotNull(message = "Number of questions is required")
    @Schema(description = "Number of questions (must be 5, 10, or 15)", example = "10", allowableValues = {"5", "10", "15"})
    private Integer numberOfQuestions;
}
