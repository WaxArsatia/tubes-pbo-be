package tubes.pbo.be.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quiz submission result")
public class QuizSubmissionResponse {
    
    @Schema(description = "Quiz ID", example = "1")
    private Long id;
    
    @Schema(description = "Total number of questions", example = "10")
    private Integer totalQuestions;
    
    @Schema(description = "Number of correct answers", example = "7")
    private Integer correctAnswers;
    
    @Schema(description = "Submission timestamp", example = "2026-01-02T10:05:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Detailed results for each question")
    private List<QuizResultItem> results;
}
