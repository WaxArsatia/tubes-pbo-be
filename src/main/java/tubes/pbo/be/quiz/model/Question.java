package tubes.pbo.be.quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_quiz_id", columnList = "quiz_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;
    
    @Column(name = "question_id", nullable = false, length = 10)
    private String questionId;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String options;  // Stored as JSON array string ["A", "B", "C", "D"]
    
    @Column(name = "correct_answer", nullable = false, length = 500)
    private String correctAnswer;
    
    @Column(name = "user_answer", length = 500)
    private String userAnswer;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;
    
    // Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;
}
