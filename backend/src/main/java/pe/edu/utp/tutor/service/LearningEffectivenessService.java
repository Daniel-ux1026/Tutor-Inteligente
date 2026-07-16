package pe.edu.utp.tutor.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.tutor.domain.AttemptEntity;
import pe.edu.utp.tutor.repository.AttemptRepository;

@Service
public class LearningEffectivenessService {
    static final int WINDOW_SIZE = 5;
    static final int ATTEMPTS_PER_COMPARISON = WINDOW_SIZE * 2;
    static final int MINIMUM_STUDENTS = 30;
    static final int MINIMUM_EVIDENCE_ATTEMPTS = 300;
    static final double MINIMUM_GAIN_POINTS = 5.0;
    static final double MINIMUM_IMPROVED_STUDENTS_PERCENT = 60.0;

    private final AttemptRepository attempts;

    public LearningEffectivenessService(AttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report() {
        return calculate(attempts.findEvidenceAttempts());
    }

    Map<String, Object> calculate(List<AttemptEntity> source) {
        List<AttemptEntity> evidence = source.stream()
            .filter(this::isEvidenceAttempt)
            .sorted(Comparator.comparing(AttemptEntity::getOccurredAt))
            .toList();

        Map<EvidenceKey, List<AttemptEntity>> byStudentAndTopic = new LinkedHashMap<>();
        for (AttemptEntity attempt : evidence) {
            EvidenceKey key = new EvidenceKey(attempt.getStudent().getId(), attempt.getQuestion().getTopic().getId());
            byStudentAndTopic.computeIfAbsent(key, ignored -> new ArrayList<>()).add(attempt);
        }

        List<TopicComparison> comparisons = byStudentAndTopic.entrySet().stream()
            .filter(entry -> entry.getValue().size() >= ATTEMPTS_PER_COMPARISON)
            .map(entry -> comparison(entry.getKey().studentId(), entry.getValue()))
            .toList();

        Map<Long, List<TopicComparison>> byStudent = new LinkedHashMap<>();
        comparisons.forEach(item -> byStudent.computeIfAbsent(item.studentId(), ignored -> new ArrayList<>()).add(item));
        List<StudentComparison> students = byStudent.entrySet().stream()
            .map(entry -> studentComparison(entry.getKey(), entry.getValue()))
            .toList();

        int totalStudents = (int) evidence.stream().map(item -> item.getStudent().getId()).distinct().count();
        int eligibleStudents = students.size();
        int evidenceAttempts = comparisons.stream().mapToInt(TopicComparison::attempts).sum();
        double baselineAccuracy = average(students.stream().map(StudentComparison::baselineAccuracy).toList());
        double recentAccuracy = average(students.stream().map(StudentComparison::recentAccuracy).toList());
        double gain = recentAccuracy - baselineAccuracy;
        double improvedStudents = eligibleStudents == 0 ? 0 : students.stream().filter(item -> item.gain() > 0).count() * 100.0 / eligibleStudents;
        double initialTime = average(students.stream().map(StudentComparison::initialTimeSeconds).toList());
        double recentTime = average(students.stream().map(StudentComparison::recentTimeSeconds).toList());

        boolean sufficient = eligibleStudents >= MINIMUM_STUDENTS && evidenceAttempts >= MINIMUM_EVIDENCE_ATTEMPTS;
        boolean efficacyObserved = sufficient && gain >= MINIMUM_GAIN_POINTS && improvedStudents >= MINIMUM_IMPROVED_STUDENTS_PERCENT;
        String status = evidence.isEmpty() ? "NO_DATA" : sufficient ? "SUFFICIENT" : "COLLECTING";
        int missingStudents = Math.max(0, MINIMUM_STUDENTS - eligibleStudents);
        int missingAttempts = Math.max(0, MINIMUM_EVIDENCE_ATTEMPTS - evidenceAttempts);
        double sampleProgress = Math.min(100, Math.min(
            eligibleStudents * 100.0 / MINIMUM_STUDENTS,
            evidenceAttempts * 100.0 / MINIMUM_EVIDENCE_ATTEMPTS));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSource", "REGISTERED_STUDENT_ACTIVITY");
        result.put("dataSourceLabel", "Actividad registrada de cuentas no demostrativas");
        result.put("generatedAt", Instant.now());
        result.put("status", status);
        result.put("efficacyObserved", efficacyObserved);
        result.put("totalStudents", totalStudents);
        result.put("eligibleStudents", eligibleStudents);
        result.put("totalAttempts", evidence.size());
        result.put("evidenceAttempts", evidenceAttempts);
        result.put("completedComparisons", comparisons.size());
        result.put("baselineAccuracy", round(baselineAccuracy));
        result.put("recentAccuracy", round(recentAccuracy));
        result.put("learningGainPoints", round(gain));
        result.put("studentsImprovedPercent", round(improvedStudents));
        result.put("initialResponseTimeSeconds", round(initialTime));
        result.put("recentResponseTimeSeconds", round(recentTime));
        result.put("minimumStudents", MINIMUM_STUDENTS);
        result.put("minimumEvidenceAttempts", MINIMUM_EVIDENCE_ATTEMPTS);
        result.put("attemptsPerComparison", ATTEMPTS_PER_COMPARISON);
        result.put("windowSize", WINDOW_SIZE);
        result.put("missingStudents", missingStudents);
        result.put("missingEvidenceAttempts", missingAttempts);
        result.put("sampleProgressPercent", round(sampleProgress));
        result.put("periodStart", evidence.isEmpty() ? "" : evidence.getFirst().getOccurredAt());
        result.put("periodEnd", evidence.isEmpty() ? "" : evidence.getLast().getOccurredAt());
        result.put("methodology", "Comparación pareada de las primeras cinco y las últimas cinco respuestas por estudiante y tema. Se excluyen las cuentas de demostración.");
        result.put("conclusion", conclusion(status, efficacyObserved, gain, improvedStudents, missingStudents, missingAttempts));
        return result;
    }

    private boolean isEvidenceAttempt(AttemptEntity attempt) {
        return attempt != null && attempt.getOccurredAt() != null && attempt.getStudent() != null
            && attempt.getStudent().getUser() != null && !attempt.getStudent().getUser().isDemo()
            && attempt.getQuestion() != null && attempt.getQuestion().getTopic() != null;
    }

    private TopicComparison comparison(Long studentId, List<AttemptEntity> orderedAttempts) {
        List<AttemptEntity> first = orderedAttempts.subList(0, WINDOW_SIZE);
        List<AttemptEntity> last = orderedAttempts.subList(orderedAttempts.size() - WINDOW_SIZE, orderedAttempts.size());
        return new TopicComparison(studentId, orderedAttempts.size(), accuracy(first), accuracy(last), averageTime(first), averageTime(last));
    }

    private StudentComparison studentComparison(Long studentId, List<TopicComparison> items) {
        double baseline = items.stream().mapToDouble(TopicComparison::baselineAccuracy).average().orElse(0);
        double recent = items.stream().mapToDouble(TopicComparison::recentAccuracy).average().orElse(0);
        double initialTime = items.stream().mapToDouble(TopicComparison::initialTimeSeconds).average().orElse(0);
        double recentTime = items.stream().mapToDouble(TopicComparison::recentTimeSeconds).average().orElse(0);
        return new StudentComparison(studentId, baseline, recent, recent - baseline, initialTime, recentTime);
    }

    private double accuracy(List<AttemptEntity> items) {
        return items.stream().filter(AttemptEntity::isCorrect).count() * 100.0 / items.size();
    }

    private double averageTime(List<AttemptEntity> items) {
        return items.stream().mapToInt(AttemptEntity::getResponseTimeSeconds).average().orElse(0);
    }

    private double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String conclusion(String status, boolean efficacyObserved, double gain, double improvedStudents,
                              int missingStudents, int missingAttempts) {
        if ("NO_DATA".equals(status)) {
            return "El indicador se activará cuando estudiantes registrados completen prácticas. Las cuentas de demostración no participan en el cálculo.";
        }
        if ("COLLECTING".equals(status)) {
            return "La evidencia se está construyendo con actividad registrada. Faltan " + missingStudents
                + " estudiantes y " + missingAttempts + " intentos comparables para emitir una conclusión estable.";
        }
        if (efficacyObserved) {
            return String.format(Locale.ROOT,
                "La cohorte analizada presenta una mejora observada de %.2f puntos porcentuales; %.2f%% de los estudiantes mejoró.",
                gain, improvedStudents);
        }
        return "La muestra alcanzó el mínimo definido, pero los resultados todavía no cumplen simultáneamente los criterios de mejora. Se recomienda revisar por curso y periodo antes de concluir eficacia.";
    }

    private record EvidenceKey(Long studentId, Long topicId) { }
    private record TopicComparison(Long studentId, int attempts, double baselineAccuracy, double recentAccuracy,
                                   double initialTimeSeconds, double recentTimeSeconds) { }
    private record StudentComparison(Long studentId, double baselineAccuracy, double recentAccuracy, double gain,
                                     double initialTimeSeconds, double recentTimeSeconds) { }
}
