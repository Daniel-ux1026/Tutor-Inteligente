package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.PasswordRecoveryTokenEntity;
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryTokenEntity, Long> { List<PasswordRecoveryTokenEntity> findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(Long userId); }
