package tubes.pbo.be.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import tubes.pbo.be.quiz.model.Question;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    private final String sampleSummaryText = "This is a sample document about Spring Boot framework.";
    private final Long testQuizId = 1L;

    @BeforeEach
    void setUp() {
        // Set aiModel via reflection since it's @Value injected
        ReflectionTestUtils.setField(questionGenerationService, "aiModel", "gemini-1.5-pro");
        // Use real ObjectMapper for JSON parsing in tests
        ReflectionTestUtils.setField(questionGenerationService, "objectMapper", new ObjectMapper());
    }

    // ===== getAiProvider Tests =====

    @Test
    void getAiProvider_returnsGemini() {
        // Act
        String provider = questionGenerationService.getAiProvider();

        // Assert
        assertNotNull(provider);
        assertEquals("gemini", provider);
    }

    // ===== getAiModel Tests =====

    @Test
    void getAiModel_returnsConfiguredModel() {
        // Act
        String model = questionGenerationService.getAiModel();

        // Assert
        assertNotNull(model);
        assertEquals("gemini-1.5-pro", model);
    }

    @Test
    void getAiModel_withDifferentModel_returnsCorrectValue() {
        // Arrange
        ReflectionTestUtils.setField(questionGenerationService, "aiModel", "gemini-1.0-pro");

        // Act
        String model = questionGenerationService.getAiModel();

        // Assert
        assertEquals("gemini-1.0-pro", model);
    }

    // ===== generateQuestions Tests =====

    @Test
    void generateQuestions_easyDifficulty_returnsQuestions() {
        // Arrange
        String aiResponse = """
                [
                  {
                    "id": "q1",
                    "question": "What is Spring Boot?",
                    "options": ["A framework", "A database", "A web server", "An IDE"],
                    "correctAnswer": "A framework",
                    "explanation": "Spring Boot is a Java framework for building applications."
                  },
                  {
                    "id": "q2",
                    "question": "What language does Spring Boot use?",
                    "options": ["Java", "Python", "C++", "Ruby"],
                    "correctAnswer": "Java",
                    "explanation": "Spring Boot is primarily used with Java."
                  }
                ]
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", 2, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(2, questions.size());
        
        Question q1 = questions.get(0);
        assertEquals("q1", q1.getQuestionId());
        assertEquals("What is Spring Boot?", q1.getQuestionText());
        assertEquals(testQuizId, q1.getQuizId());
        assertEquals("A framework", q1.getCorrectAnswer());
        assertEquals("Spring Boot is a Java framework for building applications.", q1.getExplanation());
        assertTrue(q1.getOptions().contains("A framework"));
        
        verify(chatClientBuilder).build();
    }

    @Test
    void generateQuestions_mediumDifficulty_returnsQuestions() {
        // Arrange
        String aiResponse = """
                [
                  {
                    "id": "q1",
                    "question": "How does Spring Boot simplify configuration?",
                    "options": ["Auto-configuration", "Manual setup", "XML files", "Command line"],
                    "correctAnswer": "Auto-configuration",
                    "explanation": "Spring Boot uses auto-configuration to reduce boilerplate."
                  }
                ]
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "MEDIUM", 1, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("q1", questions.get(0).getQuestionId());
        assertEquals("How does Spring Boot simplify configuration?", questions.get(0).getQuestionText());
    }

    @Test
    void generateQuestions_hardDifficulty_returnsQuestions() {
        // Arrange
        String aiResponse = """
                [
                  {
                    "id": "q1",
                    "question": "Analyze the trade-offs of using Spring Boot's embedded server.",
                    "options": ["Easier deployment vs limited control", "Faster vs slower", "Cheaper vs expensive", "Simple vs complex"],
                    "correctAnswer": "Easier deployment vs limited control",
                    "explanation": "Embedded servers simplify deployment but limit server-specific configurations."
                  }
                ]
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "HARD", 1, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("q1", questions.get(0).getQuestionId());
    }

    @Test
    void generateQuestions_withMarkdownCodeBlocks_cleansAndParsesCorrectly() {
        // Arrange
        String aiResponse = """
                ```json
                [
                  {
                    "id": "q1",
                    "question": "What is Spring Boot?",
                    "options": ["A framework", "A database", "A web server", "An IDE"],
                    "correctAnswer": "A framework",
                    "explanation": "Spring Boot is a Java framework."
                  }
                ]
                ```
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", 1, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("q1", questions.get(0).getQuestionId());
    }

    @Test
    void generateQuestions_aiFailure_throwsRuntimeException() {
        // Arrange
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec mockUserSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockUserSpec);
        when(mockUserSpec.call()).thenThrow(new RuntimeException("AI service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                questionGenerationService.generateQuestions(sampleSummaryText, "EASY", 2, testQuizId)
        );

        assertTrue(exception.getMessage().contains("Failed to generate quiz questions"));
    }

    @Test
    void generateQuestions_malformedJson_throwsRuntimeException() {
        // Arrange
        String malformedResponse = "This is not valid JSON";
        setupMockChatClient(malformedResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                questionGenerationService.generateQuestions(sampleSummaryText, "EASY", 2, testQuizId)
        );

        assertTrue(exception.getMessage().contains("Failed to generate quiz questions"));
    }

    @Test
    void generateQuestions_emptyResponse_throwsRuntimeException() {
        // Arrange
        String aiResponse = "[]";
        setupMockChatClient(aiResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                questionGenerationService.generateQuestions(
                        sampleSummaryText, "EASY", 0, testQuizId));

        // The message should contain information about empty questions
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("empty") || 
                   exception.getMessage().toLowerCase().contains("fail"));
    }

    @Test
    void generateQuestions_multipleQuestions_returnsCorrectCount() {
        // Arrange
        String aiResponse = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  },
                  {
                    "id": "q2",
                    "question": "Question 2?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "B",
                    "explanation": "Explanation 2"
                  },
                  {
                    "id": "q3",
                    "question": "Question 3?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "C",
                    "explanation": "Explanation 3"
                  }
                ]
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", 3, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(3, questions.size());
        assertEquals("q1", questions.get(0).getQuestionId());
        assertEquals("q2", questions.get(1).getQuestionId());
        assertEquals("q3", questions.get(2).getQuestionId());
    }

    @ParameterizedTest
    @MethodSource("provideDifferentNumberOfQuestionsScenarios")
    void generateQuestions_withDifferentNumberOfQuestions_handlesCorrectly(
            String scenario, String aiResponse, int requestedQuestions, int expectedSize) {
        // Arrange
        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", requestedQuestions, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(expectedSize, questions.size());
    }

    private static Stream<Arguments> provideDifferentNumberOfQuestionsScenarios() {
        String oneQuestion = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                """;

        return Stream.of(
            Arguments.of("Fewer than requested", oneQuestion, 3, 1)
        );
    }

    @Test
    void generateQuestions_noJsonArray_throwsException() {
        // Arrange
        String invalidResponse = "Just text without array";
        setupMockChatClient(invalidResponse);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                questionGenerationService.generateQuestions(sampleSummaryText, "EASY", 2, testQuizId)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidResponseScenarios")
    void generateQuestions_withInvalidResponse_throwsException(String scenario, String aiResponse) {
        // Arrange
        setupMockChatClient(aiResponse);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                questionGenerationService.generateQuestions(sampleSummaryText, "EASY", 1, testQuizId)
        );
    }

    private static Stream<Arguments> provideInvalidResponseScenarios() {
        String missingFields = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?"
                  }
                ]
                """;

        String invalidOptionsCount = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": ["A", "B", "C"],
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                """;

        String nullOptions = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": null,
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                """;

        String optionsNotList = """
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": "not a list",
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                """;

        return Stream.of(
            Arguments.of("Missing required fields", missingFields),
            Arguments.of("Invalid options count (only 3)", invalidOptionsCount),
            Arguments.of("Null options", nullOptions),
            Arguments.of("Options not a list", optionsNotList)
        );
    }

    @Test
    void generateQuestions_onlyTripleBackticks_cleansCorrectly() {
        // Arrange
        String aiResponse = """
                ```
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                ```
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", 1, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
    }

    @Test
    void generateQuestions_arrayNotAtStart_extractsCorrectly() {
        // Arrange
        String aiResponse = """
                Here is your result:
                [
                  {
                    "id": "q1",
                    "question": "Question 1?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "explanation": "Explanation 1"
                  }
                ]
                This is the end.
                """;

        setupMockChatClient(aiResponse);

        // Act
        List<Question> questions = questionGenerationService.generateQuestions(
                sampleSummaryText, "EASY", 1, testQuizId);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
    }

    // ===== Helper Methods =====

    private void setupMockChatClient(String response) {
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec mockUserSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockUserSpec);
        when(mockUserSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn(response);
    }
}
