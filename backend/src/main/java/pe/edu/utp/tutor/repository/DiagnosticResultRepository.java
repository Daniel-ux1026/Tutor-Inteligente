package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.DiagnosticResultEntity;
public interface DiagnosticResultRepository extends JpaRepository<DiagnosticResultEntity, Long> { List<DiagnosticResultEntity> findByStudentIdOrderByCompletedAtDesc(Long studentId); }
