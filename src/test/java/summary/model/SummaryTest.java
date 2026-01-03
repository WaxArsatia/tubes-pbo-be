package tubes.pbo.be.summary.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryTest {

    private Summary summary;

    @BeforeEach
    void setUp() {
        summary = new Summary();
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        // Act
        summary.onCreate();

        // Assert
        assertThat(summary.getCreatedAt()).isNotNull();
        assertThat(summary.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Long id = 1L;
        Long userId = 10L;
        User user = new User();
        String originalFilename = "test.pdf";
        String filePath = "/path/to/file.pdf";
        String summaryText = "This is a summary";
        String aiProvider = "gemini";
        String aiModel = "gemini-1.5-flash";
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        Summary testSummary = new Summary(id, userId, user, originalFilename, filePath, 
                                     summaryText, aiProvider, aiModel, createdAt);

        // Assert
        assertThat(testSummary.getId()).isEqualTo(id);
        assertThat(testSummary.getUserId()).isEqualTo(userId);
        assertThat(testSummary.getUser()).isEqualTo(user);
        assertThat(testSummary.getOriginalFilename()).isEqualTo(originalFilename);
        assertThat(testSummary.getFilePath()).isEqualTo(filePath);
        assertThat(testSummary.getSummaryText()).isEqualTo(summaryText);
        assertThat(testSummary.getAiProvider()).isEqualTo(aiProvider);
        assertThat(testSummary.getAiModel()).isEqualTo(aiModel);
        assertThat(testSummary.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        Long userId = 10L;
        User user = new User();
        String originalFilename = "document.pdf";
        String filePath = "/uploads/document.pdf";
        String summaryText = "Summary content";
        String aiProvider = "gemini";
        String aiModel = "gemini-1.5-pro";
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        summary.setId(id);
        summary.setUserId(userId);
        summary.setUser(user);
        summary.setOriginalFilename(originalFilename);
        summary.setFilePath(filePath);
        summary.setSummaryText(summaryText);
        summary.setAiProvider(aiProvider);
        summary.setAiModel(aiModel);
        summary.setCreatedAt(createdAt);

        // Assert
        assertThat(summary.getId()).isEqualTo(id);
        assertThat(summary.getUserId()).isEqualTo(userId);
        assertThat(summary.getUser()).isEqualTo(user);
        assertThat(summary.getOriginalFilename()).isEqualTo(originalFilename);
        assertThat(summary.getFilePath()).isEqualTo(filePath);
        assertThat(summary.getSummaryText()).isEqualTo(summaryText);
        assertThat(summary.getAiProvider()).isEqualTo(aiProvider);
        assertThat(summary.getAiModel()).isEqualTo(aiModel);
        assertThat(summary.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void equals_shouldWorkWithLombok() {
        // Arrange
        Summary summary1 = new Summary();
        summary1.setId(1L);
        summary1.setOriginalFilename("test.pdf");

        Summary summary2 = new Summary();
        summary2.setId(1L);
        summary2.setOriginalFilename("test.pdf");

        // Assert
        assertThat(summary1).isEqualTo(summary2);
    }

    @Test
    void hashCode_shouldWorkWithLombok() {
        // Arrange
        Summary summary1 = new Summary();
        summary1.setId(1L);
        summary1.setOriginalFilename("test.pdf");

        Summary summary2 = new Summary();
        summary2.setId(1L);
        summary2.setOriginalFilename("test.pdf");

        // Assert
        assertThat(summary1).hasSameHashCodeAs(summary2);
    }

    @Test
    void toString_shouldWorkWithLombok() {
        // Arrange
        summary.setId(1L);
        summary.setOriginalFilename("test.pdf");

        // Act
        String result = summary.toString();

        // Assert
        assertThat(result)
            .contains("Summary")
            .contains("id=1")
            .contains("originalFilename=test.pdf");
    }
}
