package tubes.pbo.be.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Question details (without correct answer before submission)")
public class QuestionResponse {
    
    @Schema(description = "Question identifier", example = "q1")
    private String id;
    
    @Schema(description = "Question text", example = "What is the main topic of this document?")
    private String question;
    
    @Schema(description = "Answer options", example = "[\"Option A\", \"Option B\", \"Option C\", \"Option D\"]")
    private List<String> options;
}
