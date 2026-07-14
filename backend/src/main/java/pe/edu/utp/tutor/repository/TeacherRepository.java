package pe.edu.utp.tutor.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.tutor.domain.TeacherEntity;
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {
    Optional<TeacherEntity> findByUserEmailIgnoreCase(String email);
    Optional<TeacherEntity> findFirstByOrderByIdAsc();
    @Query(value = "SELECT TOP 1 t.* FROM teachers t JOIN classrooms c ON c.teacher_id=t.id JOIN enrollments e ON e.classroom_id=c.id WHERE e.student_id=:studentId ORDER BY t.id", nativeQuery = true)
    Optional<TeacherEntity> findAssignedTeacher(Long studentId);
}
