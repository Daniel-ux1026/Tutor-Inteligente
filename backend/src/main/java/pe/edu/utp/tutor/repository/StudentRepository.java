package pe.edu.utp.tutor.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.StudentEntity;
public interface StudentRepository extends JpaRepository<StudentEntity, Long> { Optional<StudentEntity> findByUserEmailIgnoreCase(String email); }
