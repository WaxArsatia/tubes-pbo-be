package tubes.pbo.be.summary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tubes.pbo.be.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "summaries", indexes = {
    @Index(name = "idx_user_created", columnList = "userId, createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(nullable = false, length = 50)
    private String aiProvider;

    @Column(nullable = false, length = 100)
    private String aiModel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
