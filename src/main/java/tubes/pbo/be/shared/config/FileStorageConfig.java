package tubes.pbo.be.shared.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import tubes.pbo.be.shared.exception.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
@Slf4j
public class FileStorageConfig {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            } else {
                log.info("Upload directory already exists: {}", uploadPath);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new ConfigurationException("Could not create upload directory: " + uploadDir, e);
        }
    }

    public Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
