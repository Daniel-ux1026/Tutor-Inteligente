package pe.edu.utp.tutor.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.tutor.domain.QuestionEntity;
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findByTopicIdAndGradeAndDifficultyAndActiveTrueOrderByIdAsc(Long topicId, int grade, String difficulty);
    List<QuestionEntity> findByTopicIdAndGradeAndActiveTrueOrderByIdAsc(Long topicId, int grade);
    List<QuestionEntity> findByTopicCourseCodeAndGradeAndActiveTrueOrderByIdAsc(String courseCode, int grade);
    List<QuestionEntity> findAllByOrderByCreatedAtDesc();
    long countByTopicId(Long topicId);
}
