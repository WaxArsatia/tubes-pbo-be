package tubes.pbo.be.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tubes.pbo.be.quiz.model.Question;
import tubes.pbo.be.shared.exception.AiServiceException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGenerationService {
    
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.ai.google.genai.chat.options.model}")
    private String aiModel;
    
    public List<Question> generateQuestions(String summaryText, String difficulty, int count, Long quizId) {
        try {
            String prompt = buildPrompt(summaryText, difficulty, count);
            String response = callAiService(prompt);
            String cleanedResponse = cleanAndExtractJson(response);
            List<Map<String, Object>> questionMaps = parseJsonResponse(cleanedResponse, count);
            return convertToQuestions(questionMaps, quizId);
        } catch (Exception e) {
            log.error("Failed to generate questions with AI", e);
            throw new AiServiceException("Failed to generate quiz questions. Please try again later.", e);
        }
    }
    
    private String buildPrompt(String summaryText, String difficulty, int count) {
        return """
                Generate exactly %d multiple-choice questions based on the following document summary.
                Difficulty level: %s
                
                Requirements:
                - Each question must have exactly 4 options
                - Only ONE option should be correct
                - Include a brief explanation for the correct answer
                - Questions should test understanding, not just memorization
                - For EASY: straightforward recall questions
                - For MEDIUM: questions requiring basic analysis and understanding
                - For HARD: questions requiring deep analysis and critical thinking
                
                CRITICAL: Return ONLY a valid JSON array. Do not include any explanation, introduction, or text outside the JSON.
                Use this exact structure:
                [
                  {
                    "id": "q1",
                    "question": "Question text here?",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswer": "Option B",
                    "explanation": "Brief explanation of why this is correct"
                  }
                ]
                
                Ensure:
                - All strings are properly quoted with double quotes
                - correctAnswer must exactly match one of the options
                - No trailing commas
                - Valid JSON syntax
                
                Document summary:
                %s
                """.formatted(count, difficulty.toUpperCase(), summaryText);
    }
    
    private String callAiService(String prompt) {
        ChatClient chatClient = chatClientBuilder.build();
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("Received AI response for quiz generation");
        log.debug("Raw AI response: {}", response);
        return response;
    }
    
    private String cleanAndExtractJson(String response) {
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        cleaned = removeMarkdownBlocks(cleaned);
        
        // Extract JSON array
        int arrayStart = cleaned.indexOf('[');
        int arrayEnd = cleaned.lastIndexOf(']');
        
        if (arrayStart == -1 || arrayEnd == -1 || arrayStart > arrayEnd) {
            log.error("Invalid AI response - no valid JSON array found: {}", cleaned);
            throw new AiServiceException("AI returned invalid response format");
        }
        
        cleaned = cleaned.substring(arrayStart, arrayEnd + 1);
        log.debug("Cleaned AI response: {}", cleaned);
        return cleaned;
    }
    
    private String removeMarkdownBlocks(String text) {
        String result = text;
        if (result.startsWith("```json")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.trim();
    }
    
    private List<Map<String, Object>> parseJsonResponse(String jsonResponse, int expectedCount) {
        try {
            List<Map<String, Object>> questionMaps = objectMapper.readValue(
                    jsonResponse,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            if (questionMaps.size() != expectedCount) {
                log.warn("AI generated {} questions, expected {}. Using what was generated.",
                        questionMaps.size(), expectedCount);
            }
            
            if (questionMaps.isEmpty()) {
                throw new AiServiceException("AI returned empty question list");
            }
            
            return questionMaps;
        } catch (Exception e) {
            throw new AiServiceException("Failed to parse AI response as JSON", e);
        }
    }
    
    private List<Question> convertToQuestions(List<Map<String, Object>> questionMaps, Long quizId) {
        return questionMaps.stream()
                .map(qMap -> convertToQuestion(qMap, quizId))
                .toList();
    }
    
    private Question convertToQuestion(Map<String, Object> qMap, Long quizId) {
        validateQuestionMap(qMap);
        
        Question question = new Question();
        question.setQuizId(quizId);
        question.setQuestionId((String) qMap.get("id"));
        question.setQuestionText((String) qMap.get("question"));
        question.setOptions(serializeOptions(qMap.get("options")));
        question.setCorrectAnswer((String) qMap.get("correctAnswer"));
        question.setExplanation((String) qMap.get("explanation"));
        
        return question;
    }
    
    private void validateQuestionMap(Map<String, Object> qMap) {
        if (!qMap.containsKey("id") || !qMap.containsKey("question") ||
            !qMap.containsKey("options") || !qMap.containsKey("correctAnswer") ||
            !qMap.containsKey("explanation")) {
            log.error("Invalid question structure: {}", qMap);
            throw new AiServiceException("AI returned question with missing required fields");
        }
    }
    
    private String serializeOptions(Object optionsObj) {
        try {
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) optionsObj;
            
            if (options == null || options.size() != 4) {
                throw new AiServiceException("Question must have exactly 4 options");
            }
            
            return objectMapper.writeValueAsString(options);
        } catch (ClassCastException e) {
            throw new AiServiceException("Options must be a list of strings", e);
        } catch (Exception e) {
            throw new AiServiceException("Failed to serialize options", e);
        }
    }
    
    public String getAiProvider() {
        return "gemini";
    }
    
    public String getAiModel() {
        return aiModel;
    }
}
