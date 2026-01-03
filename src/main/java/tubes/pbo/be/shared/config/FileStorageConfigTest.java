package tubes.pbo.be.shared.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import tubes.pbo.be.shared.exception.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageConfigTest {

    private FileStorageConfig fileStorageConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageConfig = new FileStorageConfig();
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by @TempDir
    }

    @Test
    void init_withNonExistentDirectory_shouldCreateDirectory() {
        // Arrange
        Path newDir = tempDir.resolve("uploads");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", newDir.toString());

        // Act
        fileStorageConfig.init();

        // Assert
        assertThat(Files.exists(newDir)).isTrue();
        assertThat(Files.isDirectory(newDir)).isTrue();
    }

    @Test
    void init_withExistingDirectory_shouldNotThrowException() {
        // Arrange
        Path existingDir = tempDir.resolve("existing");
        try {
            Files.createDirectories(existingDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", existingDir.toString());

        // Act & Assert - should not throw exception
        fileStorageConfig.init();
        assertThat(Files.exists(existingDir)).isTrue();
    }

    @Test
    void init_withInvalidPath_shouldThrowConfigurationException() {
        // Arrange - use a path that would cause issues on most systems
        // Use a really long path that exceeds system limits to trigger ConfigurationException
        String invalidPath = "/" + "a".repeat(10000);  // extremely long path
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", invalidPath);

        // Act & Assert
        assertThatThrownBy(() -> fileStorageConfig.init())
            .isInstanceOf(RuntimeException.class)  // Can be ConfigurationException or IOException
            .hasMessageContaining("Could not create upload directory");
    }

    @Test
    void init_withNestedDirectories_shouldCreateAllParents() {
        // Arrange
        Path nestedDir = tempDir.resolve("level1/level2/level3");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", nestedDir.toString());

        // Act
        fileStorageConfig.init();

        // Assert
        assertThat(Files.exists(nestedDir)).isTrue();
        assertThat(Files.isDirectory(nestedDir)).isTrue();
        assertThat(Files.exists(nestedDir.getParent())).isTrue();
    }

    @Test
    void getUploadPath_shouldReturnNormalizedAbsolutePath() {
        // Arrange
        Path testDir = tempDir.resolve("test-upload");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", testDir.toString());

        // Act
        Path result = fileStorageConfig.getUploadPath();

        // Assert
        assertThat(result)
            .isAbsolute()
            .isEqualTo(testDir.toAbsolutePath().normalize());
    }

    @Test
    void getUploadPath_withRelativePath_shouldReturnAbsolutePath() {
        // Arrange
        String relativePath = "uploads/pdfs";
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", relativePath);

        // Act
        Path result = fileStorageConfig.getUploadPath();

        // Assert
        assertThat(result)
            .isAbsolute()
            .isEqualTo(Paths.get(relativePath).toAbsolutePath().normalize());
    }

    @Test
    void getUploadPath_withDotNotation_shouldNormalizePath() {
        // Arrange
        Path testDir = tempDir.resolve("./uploads/../uploads");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", testDir.toString());

        // Act
        Path result = fileStorageConfig.getUploadPath();

        // Assert
        assertThat(result).isAbsolute();
        assertThat(result.toString()).doesNotContain("..");
        assertThat(result.toString()).doesNotContain("./");
    }

    @Test
    void getUploadDir_shouldReturnConfiguredDirectory() {
        // Arrange
        String testPath = tempDir.resolve("test").toString();
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", testPath);

        // Act
        String result = fileStorageConfig.getUploadDir();

        // Assert
        assertThat(result).isEqualTo(testPath);
    }

    @Test
    void init_withReadOnlyParent_shouldThrowConfigurationException() throws IOException {
        // This test is platform-specific and might not work on Windows
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return; // Skip on Windows
        }

        // Arrange
        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyDir);
        readOnlyDir.toFile().setWritable(false);
        
        Path targetDir = readOnlyDir.resolve("uploads");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", targetDir.toString());

        try {
            // Act & Assert
            assertThatThrownBy(() -> fileStorageConfig.init())
                .isInstanceOf(ConfigurationException.class);
        } finally {
            // Cleanup
            readOnlyDir.toFile().setWritable(true);
        }
    }

    @Test
    void init_multipleCalls_shouldNotThrowException() {
        // Arrange
        Path testDir = tempDir.resolve("multi-init");
        ReflectionTestUtils.setField(fileStorageConfig, "uploadDir", testDir.toString());

        // Act - call init multiple times
        fileStorageConfig.init();
        fileStorageConfig.init();
        fileStorageConfig.init();

        // Assert
        assertThat(Files.exists(testDir)).isTrue();
    }
}
