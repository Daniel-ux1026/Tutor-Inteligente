package pe.edu.utp.tutor.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.ProgressEntity;
public interface ProgressRepository extends JpaRepository<ProgressEntity, Long> {
    Optional<ProgressEntity> findByStudentIdAndTopicId(Long studentId, Long topicId);
    List<ProgressEntity> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
}
