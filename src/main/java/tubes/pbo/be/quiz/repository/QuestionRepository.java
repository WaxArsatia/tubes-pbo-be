package tubes.pbo.be.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tubes.pbo.be.quiz.model.Question;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByQuizIdOrderById(Long quizId);
    
    void deleteByQuizId(Long quizId);
}
