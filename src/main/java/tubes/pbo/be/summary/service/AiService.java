package tubes.pbo.be.summary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tubes.pbo.be.shared.exception.AiServiceException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient.Builder chatClientBuilder;

    @Value("${spring.ai.google.genai.chat.options.model}")
    private String aiModel;

    public String generateSummary(String text) {
        try {
            String prompt = """
                Please provide a comprehensive and structured summary of the following document.
                Focus on key points, main arguments, and important details.
                Format the summary in clear, readable markdown with appropriate sections.
                
                Document text:
                %s
                """.formatted(text);

            ChatClient chatClient = chatClientBuilder.build();
            
            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Successfully generated summary using model: {}", aiModel);
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to generate summary with AI", e);
            throw new AiServiceException("Failed to generate summary. Please try again later.", e);
        }
    }

    public String getAiProvider() {
        return "gemini";
    }

    public String getAiModel() {
        return aiModel;
    }
}
