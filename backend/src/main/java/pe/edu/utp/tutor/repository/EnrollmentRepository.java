package pe.edu.utp.tutor.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.EnrollmentEntity;
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> { boolean existsByClassroomIdAndStudentId(Long classroomId, Long studentId); }
