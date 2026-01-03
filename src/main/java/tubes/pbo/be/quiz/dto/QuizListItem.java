package tubes.pbo.be.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Quiz list item")
public class QuizListItem {
    
    @Schema(description = "Quiz ID", example = "1")
    private Long id;
    
    @Schema(description = "Summary ID", example = "1")
    private Long summaryId;
    
    @Schema(description = "Original PDF filename", example = "document.pdf")
    private String originalFilename;
    
    @Schema(description = "Quiz difficulty", example = "medium")
    private String difficulty;
    
    @Schema(description = "Total number of questions", example = "10")
    private Integer numberOfQuestions;
    
    @Schema(description = "Whether quiz has been submitted", example = "true")
    private Boolean isSubmitted;
    
    @Schema(description = "Number of correct answers (only present if submitted)", example = "7")
    private Integer correctAnswers;
    
    @Schema(description = "Quiz creation timestamp", example = "2026-01-02T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Submission timestamp (only present if submitted)", example = "2026-01-02T10:05:00")
    private LocalDateTime submittedAt;
}
