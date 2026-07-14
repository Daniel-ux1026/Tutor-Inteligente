package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.ClassroomEntity;
public interface ClassroomRepository extends JpaRepository<ClassroomEntity, Long> { List<ClassroomEntity> findByTeacherIdOrderByGradeAsc(Long teacherId); }
