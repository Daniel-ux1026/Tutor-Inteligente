package pe.edu.utp.tutor.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.RoleEntity;
public interface RoleRepository extends JpaRepository<RoleEntity, Long> { Optional<RoleEntity> findByName(String name); }
