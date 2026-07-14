package pe.edu.utp.tutor.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.UserEntity;
public interface UserRepository extends JpaRepository<UserEntity, Long> { Optional<UserEntity> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); }
