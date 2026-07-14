package pe.edu.utp.tutor.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.AiRecommendationEntity;
public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, Long> { }
