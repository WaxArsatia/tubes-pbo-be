package tubes.pbo.be.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tubes.pbo.be.quiz.dto.*;
import tubes.pbo.be.quiz.model.Question;
import tubes.pbo.be.quiz.model.Quiz;
import tubes.pbo.be.quiz.repository.QuestionRepository;
import tubes.pbo.be.quiz.repository.QuizRepository;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private QuestionGenerationService questionGenerationService;

    @InjectMocks
    private QuizService quizService;

    private ObjectMapper objectMapper;
    private Long testUserId;
    private Long testSummaryId;
    private Summary testSummary;
    private Quiz testQuiz;
    private List<Question> testQuestions;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(quizService, "objectMapper", objectMapper);

        testUserId = 1L;
        testSummaryId = 1L;

        // Create test summary
        testSummary = new Summary();
        testSummary.setId(testSummaryId);
        testSummary.setUserId(testUserId);
        testSummary.setOriginalFilename("test-document.pdf");
        testSummary.setSummaryText("This is a sample summary text for testing.");

        // Create test quiz
        testQuiz = new Quiz();
        testQuiz.setId(1L);
        testQuiz.setUserId(testUserId);
        testQuiz.setSummaryId(testSummaryId);
        testQuiz.setDifficulty(Quiz.Difficulty.EASY);
        testQuiz.setNumberOfQuestions(5);
        testQuiz.setIsSubmitted(false);
        testQuiz.setCreatedAt(LocalDateTime.now());
        testQuiz.setSummary(testSummary);

        // Create test questions
        testQuestions = createTestQuestions(testQuiz.getId());
    }

    // ===== generateQuiz Tests =====

    @Test
    void generateQuiz_validRequest_createsQuiz() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(testSummaryId);
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(5);

        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(questionGenerationService.generateQuestions(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(testQuestions);
        when(questionRepository.saveAll(any())).thenReturn(testQuestions);

        // Act
        QuizResponse response = quizService.generateQuiz(testUserId, request);

        // Assert
        assertNotNull(response);
        assertEquals(testQuiz.getId(), response.getId());
        assertEquals(testSummaryId, response.getSummaryId());
        assertEquals("easy", response.getDifficulty());
        assertEquals(5, response.getNumberOfQuestions());
        assertFalse(response.getIsSubmitted());
        assertNull(response.getCorrectAnswers());
        assertNull(response.getSubmittedAt());
        assertEquals(5, response.getQuestions().size());

        // Verify questions don't have correct answers
        for (QuestionResponse q : response.getQuestions()) {
            assertNotNull(q.getId());
            assertNotNull(q.getQuestion());
            assertNotNull(q.getOptions());
            assertEquals(4, q.getOptions().size());
        }

        verify(summaryRepository).findByIdAndUserId(testSummaryId, testUserId);
        verify(quizRepository).save(any(Quiz.class));
        verify(questionGenerationService).generateQuestions(
                testSummary.getSummaryText(), "EASY", 5, testQuiz.getId());
        verify(questionRepository).saveAll(testQuestions);
    }

    @Test
    void generateQuiz_caseInsensitiveDifficulty_createsQuiz() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(testSummaryId);
        request.setDifficulty("medium");
        request.setNumberOfQuestions(10);

        testQuiz.setDifficulty(Quiz.Difficulty.MEDIUM);
        testQuiz.setNumberOfQuestions(10);

        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(questionGenerationService.generateQuestions(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(testQuestions);
        when(questionRepository.saveAll(any())).thenReturn(testQuestions);

        // Act
        QuizResponse response = quizService.generateQuiz(testUserId, request);

        // Assert
        assertNotNull(response);
        assertEquals("medium", response.getDifficulty());
    }

    @Test
    void generateQuiz_invalidNumberOfQuestions_throwsValidationException() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(testSummaryId);
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(7); // Invalid - must be 5, 10, or 15

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
                quizService.generateQuiz(testUserId, request));

        assertEquals("Number of questions must be exactly 5, 10, or 15", exception.getMessage());
        verify(summaryRepository, never()).findByIdAndUserId(any(), any());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void generateQuiz_summaryNotFound_throwsForbiddenException() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(testSummaryId);
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(5);

        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                quizService.generateQuiz(testUserId, request));

        assertEquals("You do not have access to this summary", exception.getMessage());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void generateQuiz_invalidDifficulty_throwsValidationException() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(testSummaryId);
        request.setDifficulty("INVALID");
        request.setNumberOfQuestions(5);

        when(summaryRepository.findByIdAndUserId(testSummaryId, testUserId))
                .thenReturn(Optional.of(testSummary));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
                quizService.generateQuiz(testUserId, request));

        assertEquals("Invalid difficulty. Must be easy, medium, or hard", exception.getMessage());
    }

    // ===== submitQuiz Tests =====

    @Test
    void submitQuiz_validSubmission_calculatesScoreCorrectly() {
        // Arrange
        Long quizId = testQuiz.getId();
        testQuiz.setQuestions(testQuestions);

        QuizSubmission submission = new QuizSubmission();
        List<QuizSubmission.QuizAnswer> answers = Arrays.asList(
                new QuizSubmission.QuizAnswer("q1", "Option A"),
                new QuizSubmission.QuizAnswer("q2", "Option B"),
                new QuizSubmission.QuizAnswer("q3", "Wrong Answer"),
                new QuizSubmission.QuizAnswer("q4", "Option D"),
                new QuizSubmission.QuizAnswer("q5", "Option A")
        );
        submission.setAnswers(answers);

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.of(testQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(questionRepository.saveAll(any())).thenReturn(testQuestions);

        // Act
        QuizSubmissionResponse response = quizService.submitQuiz(testUserId, quizId, submission);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getId());
        assertEquals(5, response.getTotalQuestions());
        assertEquals(4, response.getCorrectAnswers()); // 4 correct out of 5
        assertNotNull(response.getSubmittedAt());
        assertEquals(5, response.getResults().size());

        // Verify result details
        QuizResultItem result1 = response.getResults().get(0);
        assertEquals("q1", result1.getQuestionId());
        assertEquals("Option A", result1.getUserAnswer());
        assertEquals("Option A", result1.getCorrectAnswer());
        assertTrue(result1.getIsCorrect());
        assertNotNull(result1.getExplanation());

        QuizResultItem result3 = response.getResults().get(2);
        assertEquals("q3", result3.getQuestionId());
        assertEquals("Wrong Answer", result3.getUserAnswer());
        assertEquals("Option C", result3.getCorrectAnswer());
        assertFalse(result3.getIsCorrect());

        verify(quizRepository).save(testQuiz);
        verify(questionRepository).saveAll(testQuestions);
        assertTrue(testQuiz.getIsSubmitted());
        assertEquals(4, testQuiz.getCorrectAnswers());
    }

    @Test
    void submitQuiz_alreadySubmitted_throwsValidationException() {
        // Arrange
        Long quizId = testQuiz.getId();
        testQuiz.setIsSubmitted(true);
        testQuiz.setQuestions(testQuestions);

        QuizSubmission submission = new QuizSubmission();
        submission.setAnswers(Arrays.asList(new QuizSubmission.QuizAnswer("q1", "Option A")));

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.of(testQuiz));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
                quizService.submitQuiz(testUserId, quizId, submission));

        assertEquals("Quiz has already been submitted", exception.getMessage());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void submitQuiz_quizNotFound_throwsResourceNotFoundException() {
        // Arrange
        Long quizId = 999L;
        QuizSubmission submission = new QuizSubmission();
        submission.setAnswers(Arrays.asList(new QuizSubmission.QuizAnswer("q1", "Option A")));

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                quizService.submitQuiz(testUserId, quizId, submission));

        assertEquals("Quiz not found", exception.getMessage());
    }

    @Test
    void submitQuiz_caseInsensitiveAnswerMatching_countsCorrectly() {
        // Arrange
        Long quizId = testQuiz.getId();
        testQuiz.setQuestions(testQuestions);

        QuizSubmission submission = new QuizSubmission();
        List<QuizSubmission.QuizAnswer> answers = Arrays.asList(
                new QuizSubmission.QuizAnswer("q1", "option a"), // lowercase
                new QuizSubmission.QuizAnswer("q2", "OPTION B"), // uppercase
                new QuizSubmission.QuizAnswer("q3", " Option C "), // with spaces
                new QuizSubmission.QuizAnswer("q4", "Option D"),
                new QuizSubmission.QuizAnswer("q5", "Option A")
        );
        submission.setAnswers(answers);

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.of(testQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(questionRepository.saveAll(any())).thenReturn(testQuestions);

        // Act
        QuizSubmissionResponse response = quizService.submitQuiz(testUserId, quizId, submission);

        // Assert
        assertEquals(5, response.getCorrectAnswers()); // All correct with trimming and case-insensitive matching
    }

    // ===== listQuizzes Tests =====

    @Test
    void listQuizzes_withoutSummaryFilter_returnsAllUserQuizzes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        Page<Quiz> quizPage = new PageImpl<>(quizzes, pageable, 1);

        when(quizRepository.findByUserId(testUserId, pageable)).thenReturn(quizPage);

        // Act
        Page<QuizListItem> result = quizService.listQuizzes(testUserId, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        QuizListItem item = result.getContent().get(0);
        assertEquals(testQuiz.getId(), item.getId());
        assertEquals(testSummaryId, item.getSummaryId());
        assertEquals("test-document.pdf", item.getOriginalFilename());
        assertEquals("easy", item.getDifficulty());
        assertEquals(5, item.getNumberOfQuestions());
        assertFalse(item.getIsSubmitted());
        assertNull(item.getCorrectAnswers());
        assertNull(item.getSubmittedAt());

        verify(quizRepository).findByUserId(testUserId, pageable);
        verify(quizRepository, never()).findByUserIdAndSummaryId(any(), any(), any());
    }

    @Test
    void listQuizzes_withSummaryFilter_returnsFilteredQuizzes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        Page<Quiz> quizPage = new PageImpl<>(quizzes, pageable, 1);

        when(summaryRepository.existsByIdAndUserId(testSummaryId, testUserId)).thenReturn(true);
        when(quizRepository.findByUserIdAndSummaryId(testUserId, testSummaryId, pageable))
                .thenReturn(quizPage);

        // Act
        Page<QuizListItem> result = quizService.listQuizzes(testUserId, testSummaryId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(summaryRepository).existsByIdAndUserId(testSummaryId, testUserId);
        verify(quizRepository).findByUserIdAndSummaryId(testUserId, testSummaryId, pageable);
    }

    @Test
    void listQuizzes_summaryNotOwned_throwsForbiddenException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        when(summaryRepository.existsByIdAndUserId(testSummaryId, testUserId)).thenReturn(false);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                quizService.listQuizzes(testUserId, testSummaryId, pageable));

        assertEquals("You do not have access to this summary", exception.getMessage());
        verify(quizRepository, never()).findByUserIdAndSummaryId(any(), any(), any());
    }

    @Test
    void listQuizzes_submittedQuiz_includesScore() {
        // Arrange
        testQuiz.setIsSubmitted(true);
        testQuiz.setCorrectAnswers(4);
        testQuiz.setSubmittedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        Page<Quiz> quizPage = new PageImpl<>(quizzes, pageable, 1);

        when(quizRepository.findByUserId(testUserId, pageable)).thenReturn(quizPage);

        // Act
        Page<QuizListItem> result = quizService.listQuizzes(testUserId, null, pageable);

        // Assert
        QuizListItem item = result.getContent().get(0);
        assertTrue(item.getIsSubmitted());
        assertEquals(4, item.getCorrectAnswers());
        assertNotNull(item.getSubmittedAt());
    }

    // ===== getQuizDetail Tests =====

    @Test
    void getQuizDetail_notSubmitted_returnsQuestionsWithoutAnswers() {
        // Arrange
        Long quizId = testQuiz.getId();
        testQuiz.setQuestions(testQuestions);

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.of(testQuiz));

        // Act
        QuizResponse response = quizService.getQuizDetail(testUserId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getId());
        assertEquals(testSummaryId, response.getSummaryId());
        assertEquals("easy", response.getDifficulty());
        assertFalse(response.getIsSubmitted());
        assertNull(response.getCorrectAnswers());
        assertNull(response.getSubmittedAt());
        assertNull(response.getResults());

        // Should have questions without correct answers
        assertNotNull(response.getQuestions());
        assertEquals(5, response.getQuestions().size());
        for (QuestionResponse q : response.getQuestions()) {
            assertNotNull(q.getId());
            assertNotNull(q.getQuestion());
            assertEquals(4, q.getOptions().size());
        }
    }

    @Test
    void getQuizDetail_submitted_returnsFullResults() {
        // Arrange
        Long quizId = testQuiz.getId();
        testQuiz.setIsSubmitted(true);
        testQuiz.setCorrectAnswers(4);
        testQuiz.setSubmittedAt(LocalDateTime.now());
        
        // Set user answers on questions
        testQuestions.get(0).setUserAnswer("Option A");
        testQuestions.get(1).setUserAnswer("Option B");
        testQuestions.get(2).setUserAnswer("Wrong Answer");
        testQuestions.get(3).setUserAnswer("Option D");
        testQuestions.get(4).setUserAnswer("Option A");
        
        testQuiz.setQuestions(testQuestions);

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.of(testQuiz));

        // Act
        QuizResponse response = quizService.getQuizDetail(testUserId, quizId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getIsSubmitted());
        assertEquals(4, response.getCorrectAnswers());
        assertNotNull(response.getSubmittedAt());
        assertNull(response.getQuestions());

        // Should have results with correct answers and explanations
        assertNotNull(response.getResults());
        assertEquals(5, response.getResults().size());
        
        QuizResultItem result1 = response.getResults().get(0);
        assertTrue(result1.getIsCorrect());
        assertEquals("Option A", result1.getCorrectAnswer());
        assertNotNull(result1.getExplanation());

        QuizResultItem result3 = response.getResults().get(2);
        assertFalse(result3.getIsCorrect());
        assertEquals("Wrong Answer", result3.getUserAnswer());
        assertEquals("Option C", result3.getCorrectAnswer());
    }

    @Test
    void getQuizDetail_notFound_throwsResourceNotFoundException() {
        // Arrange
        Long quizId = 999L;

        when(quizRepository.findByIdAndUserIdWithQuestions(quizId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                quizService.getQuizDetail(testUserId, quizId));

        assertEquals("Quiz not found", exception.getMessage());
    }

    // ===== Helper Methods =====

    private List<Question> createTestQuestions(Long quizId) throws Exception {
        List<Question> questions = new ArrayList<>();
        String[] correctAnswers = {"Option A", "Option B", "Option C", "Option D", "Option A"};
        
        for (int i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setId((long) i);
            q.setQuizId(quizId);
            q.setQuestionId("q" + i);
            q.setQuestionText("Question " + i + "?");
            
            List<String> options = Arrays.asList("Option A", "Option B", "Option C", "Option D");
            q.setOptions(objectMapper.writeValueAsString(options));
            
            q.setCorrectAnswer(correctAnswers[i - 1]);
            q.setExplanation("Explanation for question " + i);
            questions.add(q);
        }
        
        return questions;
    }
}
