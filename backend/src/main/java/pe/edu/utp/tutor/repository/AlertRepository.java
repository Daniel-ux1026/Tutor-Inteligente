package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.AlertEntity;
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    List<AlertEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<AlertEntity> findAllByOrderByCreatedAtDesc();
    long countByStatusNot(String status);
}
