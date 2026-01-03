package tubes.pbo.be.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quiz result for a single question")
public class QuizResultItem {
    
    @Schema(description = "Question identifier", example = "q1")
    private String questionId;
    
    @Schema(description = "Question text", example = "What is...?")
    private String question;
    
    @Schema(description = "User's answer", example = "A")
    private String userAnswer;
    
    @Schema(description = "Correct answer", example = "B")
    private String correctAnswer;
    
    @Schema(description = "Whether user's answer was correct", example = "false")
    private Boolean isCorrect;
    
    @Schema(description = "Explanation of the correct answer")
    private String explanation;
}
