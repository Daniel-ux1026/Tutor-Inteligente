package pe.edu.utp.tutor.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.CourseEntity;
public interface CourseRepository extends JpaRepository<CourseEntity, Long> { Optional<CourseEntity> findByCodeIgnoreCase(String code); List<CourseEntity> findByActiveTrueOrderByNameAsc(); }
