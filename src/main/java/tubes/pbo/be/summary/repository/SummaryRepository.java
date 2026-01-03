package tubes.pbo.be.summary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tubes.pbo.be.summary.model.Summary;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    
    Page<Summary> findByUserId(Long userId, Pageable pageable);
    
    Optional<Summary> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
    
    // Admin: count summaries by user
    long countByUserId(Long userId);
    
    // Admin: count distinct users who have created summaries
    @Query("SELECT COUNT(DISTINCT s.userId) FROM Summary s")
    long countDistinctUsers();
    
    // Admin: count summaries created after a specific date
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Admin: get AI provider usage statistics
    @Query("SELECT s.aiProvider, COUNT(s) FROM Summary s GROUP BY s.aiProvider")
    java.util.List<Object[]> countByAiProvider();
    
    // Admin: get recent summaries with user info for activity
    @Query("SELECT s FROM Summary s ORDER BY s.createdAt DESC")
    Page<Summary> findAllOrderByCreatedAtDesc(Pageable pageable);
}
