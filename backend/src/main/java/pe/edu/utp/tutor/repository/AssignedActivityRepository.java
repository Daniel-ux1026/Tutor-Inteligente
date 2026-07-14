package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.AssignedActivityEntity;
public interface AssignedActivityRepository extends JpaRepository<AssignedActivityEntity, Long> { List<AssignedActivityEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId); List<AssignedActivityEntity> findAllByOrderByCreatedAtDesc(); }
