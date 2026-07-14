package pe.edu.utp.tutor.repository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.SyncBatchEntity;
public interface SyncBatchRepository extends JpaRepository<SyncBatchEntity, Long> { Optional<SyncBatchEntity> findByClientBatchId(UUID clientBatchId); }
