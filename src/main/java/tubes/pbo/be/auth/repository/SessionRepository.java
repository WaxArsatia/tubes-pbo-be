package tubes.pbo.be.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tubes.pbo.be.auth.model.Session;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findByToken(String token);
    
    void deleteByUserId(Long userId);
    
    void deleteByUserIdAndTokenNot(Long userId, String currentToken);

    void deleteByToken(String token);
}
