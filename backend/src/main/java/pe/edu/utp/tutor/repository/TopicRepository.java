package pe.edu.utp.tutor.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.TopicEntity;
public interface TopicRepository extends JpaRepository<TopicEntity, Long> {
    Optional<TopicEntity> findByCode(String code);
    Optional<TopicEntity> findByCourseIdAndNameIgnoreCase(Long courseId, String name);
    List<TopicEntity> findByCourseIdAndActiveTrueOrderByDisplayOrderAsc(Long courseId);
}
