package tubes.pbo.be.quiz.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tubes.pbo.be.quiz.model.Quiz;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.summary WHERE q.userId = :userId")
    Page<Quiz> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.summary WHERE q.userId = :userId AND q.summaryId = :summaryId")
    Page<Quiz> findByUserIdAndSummaryId(@Param("userId") Long userId, @Param("summaryId") Long summaryId, Pageable pageable);
    
    Optional<Quiz> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT q FROM Quiz q JOIN FETCH q.questions WHERE q.id = :id AND q.userId = :userId")
    Optional<Quiz> findByIdAndUserIdWithQuestions(@Param("id") Long id, @Param("userId") Long userId);
    
    void deleteBySummaryId(Long summaryId);
}
