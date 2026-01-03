package tubes.pbo.be.quiz.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz();
    }

    @Test
    void onCreate_shouldSetCreatedAtAndInitializeDefaults() {
        // Act
        quiz.onCreate();

        // Assert
        assertThat(quiz.getCreatedAt()).isNotNull();
        assertThat(quiz.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(quiz.getIsSubmitted()).isFalse();
    }

    @Test
    void onCreate_shouldNotOverrideIsSubmittedIfAlreadySet() {
        // Arrange
        quiz.setIsSubmitted(true);

        // Act
        quiz.onCreate();

        // Assert
        assertThat(quiz.getIsSubmitted()).isTrue();
    }

    @Test
    void onCreate_shouldSetIsSubmittedToFalseIfNull() {
        // Arrange
        quiz.setIsSubmitted(null);

        // Act
        quiz.onCreate();

        // Assert
        assertThat(quiz.getIsSubmitted()).isFalse();
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Long id = 1L;
        Long userId = 10L;
        Long summaryId = 20L;
        Quiz.Difficulty difficulty = Quiz.Difficulty.MEDIUM;
        Integer numberOfQuestions = 10;
        Integer correctAnswers = 8;
        Boolean isSubmitted = true;
        LocalDateTime submittedAt = LocalDateTime.now();
        LocalDateTime createdAt = LocalDateTime.now();
        User user = new User();
        Summary summary = new Summary();
        ArrayList<Question> questions = new ArrayList<>();

        // Act
        Quiz testQuiz = new Quiz(id, userId, summaryId, difficulty, numberOfQuestions, correctAnswers, 
                            isSubmitted, submittedAt, createdAt, user, summary, questions);

        // Assert
        assertThat(testQuiz.getId()).isEqualTo(id);
        assertThat(testQuiz.getUserId()).isEqualTo(userId);
        assertThat(testQuiz.getSummaryId()).isEqualTo(summaryId);
        assertThat(testQuiz.getDifficulty()).isEqualTo(difficulty);
        assertThat(testQuiz.getNumberOfQuestions()).isEqualTo(numberOfQuestions);
        assertThat(testQuiz.getCorrectAnswers()).isEqualTo(correctAnswers);
        assertThat(testQuiz.getIsSubmitted()).isEqualTo(isSubmitted);
        assertThat(testQuiz.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(testQuiz.getCreatedAt()).isEqualTo(createdAt);
        assertThat(testQuiz.getUser()).isEqualTo(user);
        assertThat(testQuiz.getSummary()).isEqualTo(summary);
        assertThat(testQuiz.getQuestions()).isEqualTo(questions);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        Long userId = 10L;
        Long summaryId = 20L;
        Quiz.Difficulty difficulty = Quiz.Difficulty.HARD;
        Integer numberOfQuestions = 15;
        Integer correctAnswers = 12;
        Boolean isSubmitted = true;
        LocalDateTime submittedAt = LocalDateTime.now();
        LocalDateTime createdAt = LocalDateTime.now();
        User user = new User();
        Summary summary = new Summary();
        ArrayList<Question> questions = new ArrayList<>();

        // Act
        quiz.setId(id);
        quiz.setUserId(userId);
        quiz.setSummaryId(summaryId);
        quiz.setDifficulty(difficulty);
        quiz.setNumberOfQuestions(numberOfQuestions);
        quiz.setCorrectAnswers(correctAnswers);
        quiz.setIsSubmitted(isSubmitted);
        quiz.setSubmittedAt(submittedAt);
        quiz.setCreatedAt(createdAt);
        quiz.setUser(user);
        quiz.setSummary(summary);
        quiz.setQuestions(questions);

        // Assert
        assertThat(quiz.getId()).isEqualTo(id);
        assertThat(quiz.getUserId()).isEqualTo(userId);
        assertThat(quiz.getSummaryId()).isEqualTo(summaryId);
        assertThat(quiz.getDifficulty()).isEqualTo(difficulty);
        assertThat(quiz.getNumberOfQuestions()).isEqualTo(numberOfQuestions);
        assertThat(quiz.getCorrectAnswers()).isEqualTo(correctAnswers);
        assertThat(quiz.getIsSubmitted()).isEqualTo(isSubmitted);
        assertThat(quiz.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(quiz.getCreatedAt()).isEqualTo(createdAt);
        assertThat(quiz.getUser()).isEqualTo(user);
        assertThat(quiz.getSummary()).isEqualTo(summary);
        assertThat(quiz.getQuestions()).isEqualTo(questions);
    }

    @Test
    void difficultyEnum_shouldHaveCorrectValues() {
        // Assert
        assertThat(Quiz.Difficulty.values())
            .containsExactly(Quiz.Difficulty.EASY, Quiz.Difficulty.MEDIUM, Quiz.Difficulty.HARD);
        assertThat(Quiz.Difficulty.valueOf("EASY")).isEqualTo(Quiz.Difficulty.EASY);
        assertThat(Quiz.Difficulty.valueOf("MEDIUM")).isEqualTo(Quiz.Difficulty.MEDIUM);
        assertThat(Quiz.Difficulty.valueOf("HARD")).isEqualTo(Quiz.Difficulty.HARD);
    }

    @Test
    void equals_shouldWorkWithLombok() {
        // Arrange
        Quiz quiz1 = new Quiz();
        quiz1.setId(1L);
        quiz1.setUserId(10L);

        Quiz quiz2 = new Quiz();
        quiz2.setId(1L);
        quiz2.setUserId(10L);

        // Assert
        assertThat(quiz1).isEqualTo(quiz2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        Quiz quiz1 = new Quiz();
        quiz1.setId(1L);
        quiz1.setUserId(10L);

        Quiz quiz2 = new Quiz();
        quiz2.setId(1L);
        quiz2.setUserId(10L);

        // Assert
        assertThat(quiz1).hasSameHashCodeAs(quiz2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        quiz.setId(1L);
        quiz.setDifficulty(Quiz.Difficulty.EASY);

        // Act
        String result = quiz.toString();

        // Assert
        assertThat(result)
            .contains("Quiz")
            .contains("id=1")
            .contains("difficulty=EASY");
    }
}
