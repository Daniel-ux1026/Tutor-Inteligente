package pe.edu.utp.tutor.repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.AttemptEntity;
public interface AttemptRepository extends JpaRepository<AttemptEntity, Long> {
    Optional<AttemptEntity> findByClientAttemptId(UUID clientAttemptId);
    List<AttemptEntity> findTop50ByStudentIdOrderByOccurredAtDesc(Long studentId);
    List<AttemptEntity> findTop20ByStudentIdAndQuestionTopicIdOrderByOccurredAtDesc(Long studentId, Long topicId);
    long countByStudentId(Long studentId);
}
