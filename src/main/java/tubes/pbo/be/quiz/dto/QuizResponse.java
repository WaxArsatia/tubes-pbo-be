package tubes.pbo.be.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Quiz details response")
public class QuizResponse {
    
    @Schema(description = "Quiz ID", example = "1")
    private Long id;
    
    @Schema(description = "Summary ID this quiz is based on", example = "1")
    private Long summaryId;
    
    @Schema(description = "Quiz difficulty", example = "medium")
    private String difficulty;
    
    @Schema(description = "Total number of questions", example = "10")
    private Integer numberOfQuestions;
    
    @Schema(description = "Whether quiz has been submitted", example = "false")
    private Boolean isSubmitted;
    
    @Schema(description = "Number of correct answers (only present after submission)", example = "7")
    private Integer correctAnswers;
    
    @Schema(description = "Quiz creation timestamp", example = "2026-01-02T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Quiz submission timestamp (only present after submission)", example = "2026-01-02T10:05:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "List of questions (only shown if not submitted)")
    private List<QuestionResponse> questions;
    
    @Schema(description = "Quiz results with correct answers (only shown if submitted)")
    private List<QuizResultItem> results;
}
