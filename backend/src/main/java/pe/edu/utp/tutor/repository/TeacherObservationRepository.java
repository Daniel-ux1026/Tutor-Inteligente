package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.TeacherObservationEntity;
public interface TeacherObservationRepository extends JpaRepository<TeacherObservationEntity, Long> { List<TeacherObservationEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId); }
