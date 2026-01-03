package tubes.pbo.be.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quiz submission with user answers")
public class QuizSubmission {
    
    @NotNull(message = "Answers are required")
    @NotEmpty(message = "At least one answer must be provided")
    @Schema(description = "List of answers for each question")
    private List<QuizAnswer> answers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual question answer")
    public static class QuizAnswer {
        
        @NotNull(message = "Question ID is required")
        @Schema(description = "Question identifier", example = "q1")
        private String questionId;
        
        @NotNull(message = "Answer is required")
        @Schema(description = "User's answer", example = "A")
        private String answer;
    }
}
