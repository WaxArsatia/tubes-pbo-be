package tubes.pbo.be.summary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    void setUp() {
        // Set aiModel via reflection since it's @Value injected
        ReflectionTestUtils.setField(aiService, "aiModel", "gemini-1.5-pro");
    }

    // ===== getAiProvider Tests =====

    @Test
    void getAiProvider_returnsGemini() {
        // Act
        String provider = aiService.getAiProvider();

        // Assert
        assertNotNull(provider);
        assertEquals("gemini", provider);
    }

    // ===== getAiModel Tests =====

    @Test
    void getAiModel_returnsConfiguredModel() {
        // Act
        String model = aiService.getAiModel();

        // Assert
        assertNotNull(model);
        assertEquals("gemini-1.5-pro", model);
    }

    @Test
    void getAiModel_withDifferentModel_returnsCorrectValue() {
        // Arrange
        ReflectionTestUtils.setField(aiService, "aiModel", "gemini-1.0-pro");

        // Act
        String model = aiService.getAiModel();

        // Assert
        assertEquals("gemini-1.0-pro", model);
    }

    // ===== generateSummary Tests =====

    @Test
    void generateSummary_validText_returnsGeneratedSummary() {
        // Arrange
        String inputText = "This is a sample document text that needs to be summarized.";
        String expectedSummary = "# Summary\\n\\nThis is a concise summary of the document.";
        
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec mockUserSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockUserSpec);
        when(mockUserSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn(expectedSummary);

        // Act
        String result = aiService.generateSummary(inputText);

        // Assert
        assertNotNull(result);
        assertEquals(expectedSummary, result);
        verify(chatClientBuilder).build();
        verify(mockChatClient).prompt();
        verify(mockRequestSpec).user(anyString());
        verify(mockUserSpec).call();
        verify(mockCallSpec).content();
    }

    @Test
    void generateSummary_aiServiceThrowsException_throwsRuntimeException() {
        // Arrange
        String inputText = "Sample text";
        
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        
        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenThrow(new RuntimeException("AI service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiService.generateSummary(inputText);
        });
        
        assertTrue(exception.getMessage().contains("Failed to generate summary"));
        verify(chatClientBuilder).build();
    }

    @Test
    void generateSummary_emptyResponse_returnsEmptyString() {
        // Arrange
        String inputText = "Some text";
        
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec mockUserSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockUserSpec);
        when(mockUserSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn("");

        // Act
        String result = aiService.generateSummary(inputText);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void generateSummary_longText_handlesSuccessfully() {
        // Arrange
        String longText = "A".repeat(10000); // 10k characters
        String expectedSummary = "Summary of long document";
        
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec mockUserSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockUserSpec);
        when(mockUserSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn(expectedSummary);

        // Act
        String result = aiService.generateSummary(longText);

        // Assert
        assertNotNull(result);
        assertEquals(expectedSummary, result);
    }
}
