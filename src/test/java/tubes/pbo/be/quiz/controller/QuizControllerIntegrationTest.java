package tubes.pbo.be.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tubes.pbo.be.quiz.dto.*;
import tubes.pbo.be.quiz.service.QuizService;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.shared.security.SecurityContextHelper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QuizControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    private Long testUserId;
    private QuizResponse quizResponse;
    private QuizSubmissionResponse submissionResponse;
    private List<QuizListItem> quizListItems;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Setup quiz response (not submitted)
        quizResponse = new QuizResponse();
        quizResponse.setId(1L);
        quizResponse.setSummaryId(1L);
        quizResponse.setDifficulty("easy");
        quizResponse.setNumberOfQuestions(5);
        quizResponse.setIsSubmitted(false);
        quizResponse.setCreatedAt(LocalDateTime.now());

        List<QuestionResponse> questions = Arrays.asList(
                new QuestionResponse("q1", "Question 1?", Arrays.asList("A", "B", "C", "D")),
                new QuestionResponse("q2", "Question 2?", Arrays.asList("A", "B", "C", "D")),
                new QuestionResponse("q3", "Question 3?", Arrays.asList("A", "B", "C", "D")),
                new QuestionResponse("q4", "Question 4?", Arrays.asList("A", "B", "C", "D")),
                new QuestionResponse("q5", "Question 5?", Arrays.asList("A", "B", "C", "D"))
        );
        quizResponse.setQuestions(questions);

        // Setup submission response
        submissionResponse = new QuizSubmissionResponse();
        submissionResponse.setId(1L);
        submissionResponse.setTotalQuestions(5);
        submissionResponse.setCorrectAnswers(4);
        submissionResponse.setSubmittedAt(LocalDateTime.now());

        List<QuizResultItem> results = Arrays.asList(
                createResultItem("q1", "Question 1?", "A", "A", true, "Explanation 1"),
                createResultItem("q2", "Question 2?", "B", "B", true, "Explanation 2"),
                createResultItem("q3", "Question 3?", "Wrong", "C", false, "Explanation 3"),
                createResultItem("q4", "Question 4?", "D", "D", true, "Explanation 4"),
                createResultItem("q5", "Question 5?", "A", "A", true, "Explanation 5")
        );
        submissionResponse.setResults(results);

        // Setup list items
        QuizListItem item1 = new QuizListItem();
        item1.setId(1L);
        item1.setSummaryId(1L);
        item1.setOriginalFilename("document1.pdf");
        item1.setDifficulty("easy");
        item1.setNumberOfQuestions(5);
        item1.setIsSubmitted(false);
        item1.setCreatedAt(LocalDateTime.now());

        QuizListItem item2 = new QuizListItem();
        item2.setId(2L);
        item2.setSummaryId(1L);
        item2.setOriginalFilename("document1.pdf");
        item2.setDifficulty("medium");
        item2.setNumberOfQuestions(10);
        item2.setIsSubmitted(true);
        item2.setCorrectAnswers(8);
        item2.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        item2.setSubmittedAt(LocalDateTime.now().minusSeconds(5));

        quizListItems = Arrays.asList(item1, item2);

        // Mock security context
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);
    }

    // ===== generateQuiz Tests =====

    @Test
    @WithMockUser
    void generateQuiz_validRequest_returns201() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(1L);
        request.setDifficulty("easy");
        request.setNumberOfQuestions(5);

        when(quizService.generateQuiz(anyLong(), any(QuizRequest.class)))
                .thenReturn(quizResponse);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Quiz generated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.summaryId").value(1))
                .andExpect(jsonPath("$.data.difficulty").value("easy"))
                .andExpect(jsonPath("$.data.numberOfQuestions").value(5))
                .andExpect(jsonPath("$.data.isSubmitted").value(false))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions", hasSize(5)))
                .andExpect(jsonPath("$.data.questions[0].id").value("q1"))
                .andExpect(jsonPath("$.data.questions[0].question").value("Question 1?"))
                .andExpect(jsonPath("$.data.questions[0].options", hasSize(4)))
                .andExpect(jsonPath("$.data.correctAnswers").doesNotExist())
                .andExpect(jsonPath("$.data.submittedAt").doesNotExist())
                .andExpect(jsonPath("$.data.results").doesNotExist());

        verify(securityContextHelper).getCurrentUserId();
        verify(quizService).generateQuiz(eq(testUserId), any(QuizRequest.class));
    }

    @Test
    @WithMockUser
    void generateQuiz_mediumDifficulty_returns201() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(1L);
        request.setDifficulty("medium");
        request.setNumberOfQuestions(10);

        quizResponse.setDifficulty("medium");
        quizResponse.setNumberOfQuestions(10);

        when(quizService.generateQuiz(anyLong(), any(QuizRequest.class)))
                .thenReturn(quizResponse);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.difficulty").value("medium"))
                .andExpect(jsonPath("$.data.numberOfQuestions").value(10));
    }

    @Test
    void generateQuiz_noAuth_returns403() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(1L);
        request.setDifficulty("easy");
        request.setNumberOfQuestions(5);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(quizService, never()).generateQuiz(anyLong(), any());
    }

    @Test
    @WithMockUser
    void generateQuiz_invalidNumberOfQuestions_returns400() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(1L);
        request.setDifficulty("easy");
        request.setNumberOfQuestions(7); // Invalid

        when(quizService.generateQuiz(anyLong(), any(QuizRequest.class)))
                .thenThrow(new ValidationException("Number of questions must be exactly 5, 10, or 15"));

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Number of questions must be exactly 5, 10, or 15"));
    }

    @Test
    @WithMockUser
    void generateQuiz_invalidDifficulty_returns400() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(1L);
        request.setDifficulty("INVALID");
        request.setNumberOfQuestions(5);

        // Act & Assert - validation happens at DTO level
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Difficulty must be easy, medium, or hard")));
    }

    @Test
    @WithMockUser
    void generateQuiz_summaryNotOwned_returns403() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setSummaryId(999L);
        request.setDifficulty("easy");
        request.setNumberOfQuestions(5);

        when(quizService.generateQuiz(anyLong(), any(QuizRequest.class)))
                .thenThrow(new ForbiddenException("You do not have access to this summary"));

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have access to this summary"));
    }

    // ===== submitQuiz Tests =====

    @Test
    @WithMockUser
    void submitQuiz_validSubmission_returns200() throws Exception {
        // Arrange
        QuizSubmission submission = new QuizSubmission();
        List<QuizSubmission.QuizAnswer> answers = Arrays.asList(
                new QuizSubmission.QuizAnswer("q1", "A"),
                new QuizSubmission.QuizAnswer("q2", "B"),
                new QuizSubmission.QuizAnswer("q3", "Wrong"),
                new QuizSubmission.QuizAnswer("q4", "D"),
                new QuizSubmission.QuizAnswer("q5", "A")
        );
        submission.setAnswers(answers);

        when(quizService.submitQuiz(anyLong(), anyLong(), any(QuizSubmission.class)))
                .thenReturn(submissionResponse);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Quiz submitted successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.totalQuestions").value(5))
                .andExpect(jsonPath("$.data.correctAnswers").value(4))
                .andExpect(jsonPath("$.data.submittedAt").exists())
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results", hasSize(5)))
                .andExpect(jsonPath("$.data.results[0].questionId").value("q1"))
                .andExpect(jsonPath("$.data.results[0].userAnswer").value("A"))
                .andExpect(jsonPath("$.data.results[0].correctAnswer").value("A"))
                .andExpect(jsonPath("$.data.results[0].isCorrect").value(true))
                .andExpect(jsonPath("$.data.results[0].explanation").value("Explanation 1"))
                .andExpect(jsonPath("$.data.results[2].isCorrect").value(false));

        verify(quizService).submitQuiz(eq(testUserId), eq(1L), any(QuizSubmission.class));
    }

    @Test
    void submitQuiz_noAuth_returns403() throws Exception {
        // Arrange
        QuizSubmission submission = new QuizSubmission();
        submission.setAnswers(Arrays.asList(new QuizSubmission.QuizAnswer("q1", "A")));

        // Act & Assert
        mockMvc.perform(post("/api/quizzes/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isForbidden());

        verify(quizService, never()).submitQuiz(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockUser
    void submitQuiz_alreadySubmitted_returns400() throws Exception {
        // Arrange
        QuizSubmission submission = new QuizSubmission();
        submission.setAnswers(Arrays.asList(new QuizSubmission.QuizAnswer("q1", "A")));

        when(quizService.submitQuiz(anyLong(), anyLong(), any(QuizSubmission.class)))
                .thenThrow(new ValidationException("Quiz has already been submitted"));

        // Act & Assert
        mockMvc.perform(post("/api/quizzes/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Quiz has already been submitted"));
    }

    @Test
    @WithMockUser
    void submitQuiz_quizNotFound_returns404() throws Exception {
        // Arrange
        QuizSubmission submission = new QuizSubmission();
        submission.setAnswers(Arrays.asList(new QuizSubmission.QuizAnswer("q1", "A")));

        when(quizService.submitQuiz(anyLong(), anyLong(), any(QuizSubmission.class)))
                .thenThrow(new ResourceNotFoundException("Quiz not found"));

        // Act & Assert
        mockMvc.perform(post("/api/quizzes/999/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Quiz not found"));
    }

    // ===== listQuizzes Tests =====

    @Test
    @WithMockUser
    void listQuizzes_withoutFilter_returns200() throws Exception {
        // Arrange
        Page<QuizListItem> page = new PageImpl<>(quizListItems, PageRequest.of(0, 10), 2);
        when(quizService.listQuizzes(anyLong(), isNull(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/quizzes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].summaryId").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("document1.pdf"))
                .andExpect(jsonPath("$.content[0].difficulty").value("easy"))
                .andExpect(jsonPath("$.content[0].numberOfQuestions").value(5))
                .andExpect(jsonPath("$.content[0].isSubmitted").value(false))
                .andExpect(jsonPath("$.content[0].correctAnswers").doesNotExist())
                .andExpect(jsonPath("$.content[1].isSubmitted").value(true))
                .andExpect(jsonPath("$.content[1].correctAnswers").value(8))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(quizService).listQuizzes(eq(testUserId), isNull(), any());
    }

    @Test
    @WithMockUser
    void listQuizzes_withSummaryFilter_returns200() throws Exception {
        // Arrange
        Page<QuizListItem> page = new PageImpl<>(quizListItems, PageRequest.of(0, 10), 2);
        when(quizService.listQuizzes(anyLong(), eq(1L), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/quizzes")
                        .param("summaryId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        verify(quizService).listQuizzes(eq(testUserId), eq(1L), any());
    }

    @Test
    void listQuizzes_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isForbidden());

        verify(quizService, never()).listQuizzes(anyLong(), any(), any());
    }

    @Test
    @WithMockUser
    void listQuizzes_summaryNotOwned_returns403() throws Exception {
        // Arrange
        when(quizService.listQuizzes(anyLong(), anyLong(), any()))
                .thenThrow(new ForbiddenException("You do not have access to this summary"));

        // Act & Assert
        mockMvc.perform(get("/api/quizzes")
                        .param("summaryId", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this summary"));
    }

    // ===== getQuizDetail Tests =====

    @Test
    @WithMockUser
    void getQuizDetail_notSubmitted_returns200WithQuestionsOnly() throws Exception {
        // Arrange
        when(quizService.getQuizDetail(anyLong(), anyLong()))
                .thenReturn(quizResponse);

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Quiz retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isSubmitted").value(false))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions", hasSize(5)))
                .andExpect(jsonPath("$.data.correctAnswers").doesNotExist())
                .andExpect(jsonPath("$.data.submittedAt").doesNotExist())
                .andExpect(jsonPath("$.data.results").doesNotExist());

        verify(quizService).getQuizDetail(testUserId, 1L);
    }

    @Test
    @WithMockUser
    void getQuizDetail_submitted_returns200WithResults() throws Exception {
        // Arrange
        QuizResponse submittedQuiz = new QuizResponse();
        submittedQuiz.setId(1L);
        submittedQuiz.setSummaryId(1L);
        submittedQuiz.setDifficulty("easy");
        submittedQuiz.setNumberOfQuestions(5);
        submittedQuiz.setIsSubmitted(true);
        submittedQuiz.setCorrectAnswers(4);
        submittedQuiz.setCreatedAt(LocalDateTime.now());
        submittedQuiz.setSubmittedAt(LocalDateTime.now());
        submittedQuiz.setResults(submissionResponse.getResults());

        when(quizService.getQuizDetail(anyLong(), anyLong()))
                .thenReturn(submittedQuiz);

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSubmitted").value(true))
                .andExpect(jsonPath("$.data.correctAnswers").value(4))
                .andExpect(jsonPath("$.data.submittedAt").exists())
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results", hasSize(5)))
                .andExpect(jsonPath("$.data.results[0].correctAnswer").exists())
                .andExpect(jsonPath("$.data.results[0].explanation").exists())
                .andExpect(jsonPath("$.data.questions").doesNotExist());
    }

    @Test
    void getQuizDetail_noAuth_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isForbidden());

        verify(quizService, never()).getQuizDetail(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    void getQuizDetail_notFound_returns404() throws Exception {
        // Arrange
        when(quizService.getQuizDetail(anyLong(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Quiz not found"));

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Quiz not found"));
    }

    // ===== Helper Methods =====

    private QuizResultItem createResultItem(String questionId, String question, 
                                           String userAnswer, String correctAnswer, 
                                           boolean isCorrect, String explanation) {
        QuizResultItem item = new QuizResultItem();
        item.setQuestionId(questionId);
        item.setQuestion(question);
        item.setUserAnswer(userAnswer);
        item.setCorrectAnswer(correctAnswer);
        item.setIsCorrect(isCorrect);
        item.setExplanation(explanation);
        return item;
    }
}
