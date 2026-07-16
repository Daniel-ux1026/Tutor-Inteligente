package pe.edu.utp.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.utp.tutor.domain.AttemptEntity;
import pe.edu.utp.tutor.domain.QuestionEntity;
import pe.edu.utp.tutor.domain.StudentEntity;
import pe.edu.utp.tutor.domain.TopicEntity;
import pe.edu.utp.tutor.domain.UserEntity;
import pe.edu.utp.tutor.repository.AttemptRepository;
import pe.edu.utp.tutor.service.LearningEffectivenessService;

class LearningEffectivenessServiceTest {
    @Test
    void reportsObservedImprovementOnlyWithSufficientNonDemoEvidence() {
        AttemptRepository repository = mock(AttemptRepository.class);
        List<AttemptEntity> evidence = new ArrayList<>();
        for (long studentId = 1; studentId <= 30; studentId++) {
            for (int index = 0; index < 10; index++) evidence.add(attempt(studentId, false, index));
        }
        for (int index = 0; index < 10; index++) evidence.add(attempt(99, true, index));
        when(repository.findEvidenceAttempts()).thenReturn(evidence);

        Map<String, Object> report = new LearningEffectivenessService(repository).report();

        assertThat(report.get("status")).isEqualTo("SUFFICIENT");
        assertThat(report.get("efficacyObserved")).isEqualTo(true);
        assertThat(report.get("totalStudents")).isEqualTo(30);
        assertThat(report.get("totalAttempts")).isEqualTo(300);
        assertThat(((Number) report.get("baselineAccuracy")).doubleValue()).isEqualTo(40.0);
        assertThat(((Number) report.get("recentAccuracy")).doubleValue()).isEqualTo(80.0);
        assertThat(((Number) report.get("learningGainPoints")).doubleValue()).isEqualTo(40.0);
        assertThat(((Number) report.get("studentsImprovedPercent")).doubleValue()).isEqualTo(100.0);
    }

    private AttemptEntity attempt(long studentId, boolean demo, int index) {
        UserEntity user = new UserEntity();
        user.setId(studentId); user.setDemo(demo);
        StudentEntity student = new StudentEntity();
        student.setId(studentId); student.setUser(user);
        TopicEntity topic = new TopicEntity(); topic.setId(1L);
        QuestionEntity question = new QuestionEntity(); question.setId((long) index + 1); question.setTopic(topic);
        AttemptEntity attempt = new AttemptEntity();
        attempt.setClientAttemptId(UUID.randomUUID()); attempt.setStudent(student); attempt.setQuestion(question);
        attempt.setCorrect(index < 2 || (index >= 5 && index < 9));
        attempt.setResponseTimeSeconds(index < 5 ? 80 : 60);
        attempt.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(studentId * 100 + index));
        return attempt;
    }
}
