package pe.edu.utp.tutor.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.AlternativeEntity;
public interface AlternativeRepository extends JpaRepository<AlternativeEntity, Long> { }
