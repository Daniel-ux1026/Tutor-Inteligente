package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.NotificationEntity;
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findTop50ByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    long countByTeacherIdAndReadAtIsNull(Long teacherId);
}
