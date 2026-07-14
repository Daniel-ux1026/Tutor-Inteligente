package pe.edu.utp.tutor.service;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.tutor.domain.*;
import pe.edu.utp.tutor.repository.*;

@Service
public class StudentService {
    private final StudentRepository students; private final CourseRepository courses; private final TopicRepository topics;
    private final QuestionRepository questions; private final AlternativeRepository alternatives; private final AttemptRepository attempts;
    private final ProgressRepository progress; private final DiagnosticResultRepository diagnostics; private final AiRecommendationRepository recommendations;
    private final AlertRepository alerts; private final SyncBatchRepository syncBatches; private final AiClient ai;
    private final AssignedActivityRepository activities; private final TeacherObservationRepository observations;
    private final NotificationService notificationService; private final ObjectMapper mapper;

    public StudentService(StudentRepository students, CourseRepository courses, TopicRepository topics, QuestionRepository questions,
                          AlternativeRepository alternatives, AttemptRepository attempts, ProgressRepository progress,
                           DiagnosticResultRepository diagnostics, AiRecommendationRepository recommendations, AlertRepository alerts,
                           SyncBatchRepository syncBatches, AiClient ai, AssignedActivityRepository activities,
                           TeacherObservationRepository observations, NotificationService notificationService, ObjectMapper mapper) {
        this.students=students; this.courses=courses; this.topics=topics; this.questions=questions; this.alternatives=alternatives;
        this.attempts=attempts; this.progress=progress; this.diagnostics=diagnostics; this.recommendations=recommendations;
        this.alerts=alerts; this.syncBatches=syncBatches; this.ai=ai; this.activities=activities; this.observations=observations;
        this.notificationService=notificationService; this.mapper=mapper;
    }

    public Map<String, Object> profile(String email) {
        StudentEntity s = student(email);
        return Map.of("id", s.getId(), "fullName", s.getUser().getFullName(), "email", s.getUser().getEmail(), "grade", s.getGrade(), "section", s.getSection());
    }

    public List<Map<String, Object>> courses() {
        return courses.findByActiveTrueOrderByNameAsc().stream().map(c -> Map.<String,Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName())).toList();
    }

    public List<Map<String, Object>> topics(Long courseId) {
        return topics.findByCourseIdAndActiveTrueOrderByDisplayOrderAsc(courseId).stream().map(t -> Map.<String,Object>of("id", t.getId(), "code", t.getCode(), "name", t.getName(), "course", t.getCourse().getName())).toList();
    }

    @Transactional
    public Map<String, Object> diagnostic(String email, DiagnosticRequest request) {
        StudentEntity s = student(email); CourseEntity course = courses.findById(request.courseId()).orElseThrow(() -> notFound("Curso"));
        String level = request.scorePercent() >= 80 ? "avanzado" : request.scorePercent() >= 50 ? "intermedio" : "basico";
        DiagnosticResultEntity result = new DiagnosticResultEntity(); result.setStudent(s); result.setCourse(course);
        result.setScorePercent(BigDecimal.valueOf(request.scorePercent())); result.setInitialLevel(level);
        try { result.setAnswersJson(mapper.writeValueAsString(request.answers())); } catch (Exception ex) { result.setAnswersJson("{}"); }
        diagnostics.save(result);
        for (TopicEntity topic : topics.findByCourseIdAndActiveTrueOrderByDisplayOrderAsc(course.getId())) {
            ProgressEntity p = progress.findByStudentIdAndTopicId(s.getId(), topic.getId()).orElseGet(ProgressEntity::new);
            if (p.getId() == null) { p.setStudent(s); p.setTopic(topic); }
            p.setCurrentLevel(level); p.setRecommendedLevel(level); p.setUpdatedAt(Instant.now()); progress.save(p);
        }
        return Map.of("scorePercent", request.scorePercent(), "initialLevel", level, "message", "Diagnóstico guardado y nivel inicial aplicado.");
    }

    public List<Map<String,Object>> diagnostics(String email) {
        Map<Long,DiagnosticResultEntity> latest = new LinkedHashMap<>();
        diagnostics.findByStudentIdOrderByCompletedAtDesc(student(email).getId()).forEach(item -> latest.putIfAbsent(item.getCourse().getId(), item));
        return latest.values().stream().map(item -> Map.<String,Object>of("courseId",item.getCourse().getId(),"courseCode",item.getCourse().getCode(),"course",item.getCourse().getName(),"scorePercent",item.getScorePercent(),"initialLevel",item.getInitialLevel(),"completedAt",item.getCompletedAt())).toList();
    }

    @Transactional
    public Map<String, Object> nextExercise(String email, Long topicId) {
        StudentEntity s = student(email); TopicEntity topic = topics.findById(topicId).orElseThrow(() -> notFound("Tema"));
        ProgressEntity p = progress.findByStudentIdAndTopicId(s.getId(), topicId).orElse(null);
        List<AttemptEntity> recent = attempts.findTop20ByStudentIdAndQuestionTopicIdOrderByOccurredAtDesc(s.getId(), topicId);
        AiPrediction prediction = ai.predict(features(p, recent, null, null)); persistRecommendation(s, topic, prediction, features(p, recent, null, null));
        List<QuestionEntity> pool = questions.findByTopicIdAndGradeAndDifficultyAndActiveTrueOrderByIdAsc(topicId, s.getGrade(), prediction.recommendedLevel());
        if (pool.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay ejercicios activos para este tema y grado.");
        QuestionEntity q = pool.get((int)(attempts.countByStudentId(s.getId()) % pool.size()));
        return questionDto(q, prediction);
    }

    @Transactional
    public List<Map<String,Object>> offlineExercises(String email, Long topicId, int requestedLimit) {
        StudentEntity s = student(email); TopicEntity topic = topics.findById(topicId).orElseThrow(() -> notFound("Tema"));
        ProgressEntity p = progress.findByStudentIdAndTopicId(s.getId(), topicId).orElse(null);
        List<AttemptEntity> recent = attempts.findTop20ByStudentIdAndQuestionTopicIdOrderByOccurredAtDesc(s.getId(), topicId);
        AiFeatures input = features(p, recent, null, null); AiPrediction prediction = ai.predict(input);
        persistRecommendation(s, topic, prediction, input);
        int limit = Math.max(1, Math.min(30, requestedLimit));
        List<QuestionEntity> pool = new ArrayList<>(questions.findByTopicIdAndGradeAndActiveTrueOrderByIdAsc(topicId, s.getGrade()));
        pool.sort(Comparator.comparing((QuestionEntity q) -> !q.getDifficulty().equals(prediction.recommendedLevel())).thenComparing(QuestionEntity::getId));
        if (pool.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay ejercicios activos para este tema y grado.");
        return pool.stream().limit(limit).map(q -> questionDto(q, prediction)).toList();
    }

    @Transactional
    public Map<String, Object> submit(String email, AttemptRequest request) { return saveAttempt(student(email), request); }

    @Transactional
    public SyncResponse sync(String email, SyncRequest request) {
        StudentEntity s = student(email);
        Optional<SyncBatchEntity> existingBatch = syncBatches.findByClientBatchId(request.clientBatchId());
        if (existingBatch.isPresent()) {
            List<UUID> all = request.attempts().stream().map(AttemptRequest::clientAttemptId).toList();
            return new SyncResponse(request.clientBatchId(), all, all, existingBatch.get().getCreatedAt());
        }
        List<UUID> confirmed = new ArrayList<>(); List<UUID> duplicated = new ArrayList<>();
        TopicEntity lastTopic = null;
        for (AttemptRequest item : request.attempts()) {
            if (attempts.findByClientAttemptId(item.clientAttemptId()).isPresent()) duplicated.add(item.clientAttemptId());
            else saveAttempt(s, item);
            confirmed.add(item.clientAttemptId());
            lastTopic = questions.findById(item.questionId()).map(QuestionEntity::getTopic).orElse(lastTopic);
        }
        SyncBatchEntity batch = new SyncBatchEntity(); batch.setClientBatchId(request.clientBatchId()); batch.setStudent(s);
        batch.setReceivedCount(request.attempts().size()); batch.setConfirmedCount(confirmed.size()); syncBatches.save(batch);
        if (lastTopic != null) notificationService.create(s, lastTopic, null, null, "SYNC_COMPLETED",
            "Sincronizó " + confirmed.size() + " respuestas trabajadas sin conexión.", "MEDIUM");
        return new SyncResponse(request.clientBatchId(), confirmed, duplicated, batch.getCreatedAt());
    }

    public Map<String, Object> progress(String email) { return progressDto(student(email)); }
    public List<Map<String, Object>> history(String email) { return attempts.findTop50ByStudentIdOrderByOccurredAtDesc(student(email).getId()).stream().map(this::attemptDto).toList(); }
    public List<Map<String,Object>> activities(String email) {
        return activities.findByStudentIdOrderByCreatedAtDesc(student(email).getId()).stream().map(a -> {
            Map<String,Object> item = new LinkedHashMap<>(); item.put("id",a.getId()); item.put("title",a.getTitle()); item.put("instructions",a.getInstructions());
            item.put("topicId",a.getTopic().getId()); item.put("topic",a.getTopic().getName()); item.put("courseId",a.getTopic().getCourse().getId()); item.put("course",a.getTopic().getCourse().getName());
            item.put("dueAt",a.getDueAt()); item.put("status",a.getStatus()); item.put("createdAt",a.getCreatedAt()); return item;
        }).toList();
    }

    public List<Map<String,Object>> notifications(String email) {
        return observations.findByStudentIdOrderByCreatedAtDesc(student(email).getId()).stream().map(observation -> {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("id", observation.getId());
            item.put("type", "PEDAGOGICAL_RECOMMENDATION");
            item.put("title", "Recomendación pedagógica");
            item.put("text", observation.getText());
            item.put("teacher", observation.getTeacher().getUser().getFullName());
            item.put("createdAt", observation.getCreatedAt());
            return item;
        }).toList();
    }

    private Map<String, Object> saveAttempt(StudentEntity s, AttemptRequest request) {
        Optional<AttemptEntity> duplicate = attempts.findByClientAttemptId(request.clientAttemptId());
        if (duplicate.isPresent()) { Map<String,Object> dto = new LinkedHashMap<>(attemptDto(duplicate.get())); dto.put("duplicate", true); return dto; }
        QuestionEntity q = questions.findById(request.questionId()).orElseThrow(() -> notFound("Pregunta"));
        AlternativeEntity selected = alternatives.findById(request.selectedAlternativeId()).orElseThrow(() -> notFound("Alternativa"));
        if (!selected.getQuestion().getId().equals(q.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La alternativa no pertenece a la pregunta.");
        ProgressEntity p = progress.findByStudentIdAndTopicId(s.getId(), q.getTopic().getId()).orElseGet(() -> newProgress(s, q.getTopic()));
        List<AttemptEntity> recent = attempts.findTop20ByStudentIdAndQuestionTopicIdOrderByOccurredAtDesc(s.getId(), q.getTopic().getId());
        AiFeatures f = features(p, recent, selected.isCorrect(), request.responseTimeSeconds()); AiPrediction prediction = ai.predict(f);
        persistRecommendation(s, q.getTopic(), prediction, f);
        String previous = p.getCurrentLevel();
        AttemptEntity a = new AttemptEntity(); a.setClientAttemptId(request.clientAttemptId()); a.setStudent(s); a.setQuestion(q); a.setSelectedAlternative(selected);
        a.setCorrect(selected.isCorrect()); a.setResponseTimeSeconds(request.responseTimeSeconds()); a.setLevelBefore(previous);
        a.setRecommendedLevel(prediction.recommendedLevel()); a.setAiExplanation(prediction.explanation()); a.setRecommendationSource(prediction.source());
        a.setOccurredAt(request.occurredAt()); attempts.save(a);
        updateProgress(p, a); progress.save(p);
        AlertEntity alert = evaluateAlert(s, q.getTopic(), p, a, previous);
        if (alert != null) notificationService.create(s, q.getTopic(), a, alert, "LOW_PERFORMANCE", alert.getReason(), "HIGH");
        else if (!Objects.equals(previous, p.getCurrentLevel())) notificationService.create(s, q.getTopic(), a, null, "LEVEL_CHANGED", "Cambió de nivel: " + previous + " → " + p.getCurrentLevel() + ".", "HIGH");
        else if (p.getTotalAttempts() % 5 == 0) notificationService.create(s, q.getTopic(), a, null, "ACTIVITY_COMPLETED", "Completó un bloque de cinco ejercicios con " + accuracy(p) + "% de aciertos.", "MEDIUM");
        Map<String,Object> result = new LinkedHashMap<>(attemptDto(a)); result.put("answerExplanation", q.getExplanation()); result.put("progress", progressItem(p)); result.put("alertGenerated", alert != null);
        return result;
    }

    private void updateProgress(ProgressEntity p, AttemptEntity a) {
        int oldTotal = p.getTotalAttempts(); p.setTotalAttempts(oldTotal + 1);
        if (a.isCorrect()) { p.setCorrectAttempts(p.getCorrectAttempts() + 1); p.setSuccessStreak(p.getSuccessStreak() + 1); p.setConsecutiveErrors(0); }
        else { p.setSuccessStreak(0); p.setConsecutiveErrors(p.getConsecutiveErrors() + 1); }
        double avg = (p.getAverageTimeSeconds().doubleValue() * oldTotal + a.getResponseTimeSeconds()) / (oldTotal + 1);
        p.setAverageTimeSeconds(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        p.setCurrentLevel(a.getRecommendedLevel()); p.setRecommendedLevel(a.getRecommendedLevel()); p.setUpdatedAt(Instant.now());
    }

    private AlertEntity evaluateAlert(StudentEntity s, TopicEntity topic, ProgressEntity p, AttemptEntity a, String previous) {
        String type=null, reason=null, risk="MEDIUM", recommendation=null;
        if (p.getConsecutiveErrors() >= 2) { type="CONSECUTIVE_ERRORS"; reason="Acumula " + p.getConsecutiveErrors() + " errores consecutivos en " + topic.getName() + "."; risk="HIGH"; recommendation="Asignar refuerzo guiado y revisar el procedimiento, no solo la respuesta."; }
        else if (p.getTotalAttempts() >= 3 && accuracy(p) < 60) { type="LOW_PERFORMANCE"; reason="Rendimiento de " + accuracy(p) + "%: menor al 60% esperado."; risk="HIGH"; recommendation="Revisar prerrequisitos y asignar una actividad breve de nivel básico."; }
        else if (levelIndex(p.getCurrentLevel()) < levelIndex(previous)) { type="LEVEL_DECREASE"; reason="La recomendación descendió de " + previous + " a " + p.getCurrentLevel() + "."; recommendation="Analizar las últimas cinco respuestas antes de continuar."; }
        else if (a.getResponseTimeSeconds() > 120) { type="EXCESSIVE_TIME"; reason="El tiempo de respuesta fue " + a.getResponseTimeSeconds() + " segundos."; recommendation="Comprobar comprensión de consignas y ofrecer un ejemplo resuelto."; }
        else if (p.getTotalAttempts() >= 10 && p.getTotalAttempts() % 5 == 0) { type="TOPIC_REPETITION"; reason="Practica el mismo tema con frecuencia (" + p.getTotalAttempts() + " intentos)."; recommendation="Alternar con un tema relacionado y revisar transferencia de aprendizaje."; }
        if (type == null) return null;
        String alertType = type;
        boolean alreadyOpen = alerts.findByStudentIdOrderByCreatedAtDesc(s.getId()).stream().anyMatch(x -> x.getTopic().getId().equals(topic.getId()) && x.getAlertType().equals(alertType) && !"RESOLVED".equals(x.getStatus()));
        if (alreadyOpen) return null;
        AlertEntity alert = new AlertEntity(); alert.setStudent(s); alert.setCourse(topic.getCourse()); alert.setTopic(topic);
        alert.setAlertType(type); alert.setReason(reason); alert.setRiskLevel(risk); alert.setPedagogicalRecommendation(recommendation); return alerts.save(alert);
    }

    private AiFeatures features(ProgressEntity p, List<AttemptEntity> recent, Boolean currentCorrect, Integer currentTime) {
        List<Boolean> correctness = new ArrayList<>(); if (currentCorrect != null) correctness.add(currentCorrect);
        recent.stream().limit(currentCorrect == null ? 5 : 4).forEach(a -> correctness.add(a.isCorrect()));
        int correct = (int) correctness.stream().filter(Boolean::booleanValue).count();
        double avg = currentTime == null ? recent.stream().limit(5).mapToInt(AttemptEntity::getResponseTimeSeconds).average().orElse(p == null ? 60 : p.getAverageTimeSeconds().doubleValue())
            : (currentTime + recent.stream().limit(4).mapToInt(AttemptEntity::getResponseTimeSeconds).sum()) / (double)Math.max(1, Math.min(5, recent.size()+1));
        int errors = currentCorrect == null ? (p == null ? 0 : p.getConsecutiveErrors()) : currentCorrect ? 0 : (p == null ? 1 : p.getConsecutiveErrors()+1);
        int streak = currentCorrect == null ? (p == null ? 0 : p.getSuccessStreak()) : currentCorrect ? (p == null ? 1 : p.getSuccessStreak()+1) : 0;
        return new AiFeatures(p == null ? "basico" : p.getCurrentLevel(), correct, errors, avg, (p == null ? 0 : p.getTotalAttempts()) + (currentCorrect == null ? 0 : 1), streak);
    }

    private void persistRecommendation(StudentEntity s, TopicEntity topic, AiPrediction prediction, AiFeatures f) {
        AiRecommendationEntity entity = new AiRecommendationEntity(); entity.setStudent(s); entity.setTopic(topic);
        entity.setRecommendedLevel(prediction.recommendedLevel()); entity.setExplanation(prediction.explanation()); entity.setModelVersion(prediction.modelVersion()); entity.setSource(prediction.source());
        try { entity.setInputJson(mapper.writeValueAsString(f)); } catch (Exception ex) { entity.setInputJson("{}"); } recommendations.save(entity);
    }

    private Map<String,Object> questionDto(QuestionEntity q, AiPrediction p) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("id", q.getId()); result.put("courseId", q.getTopic().getCourse().getId()); result.put("course", q.getTopic().getCourse().getName()); result.put("topicId", q.getTopic().getId()); result.put("topic", q.getTopic().getName());
        result.put("grade", q.getGrade()); result.put("difficulty", q.getDifficulty()); result.put("prompt", q.getPrompt());
        result.put("alternatives", q.getAlternatives().stream().map(a -> Map.of("id", a.getId(), "label", a.getLabel(), "text", a.getText())).toList());
        result.put("offlineAnswerKey", q.getAlternatives().stream().filter(AlternativeEntity::isCorrect).map(AlternativeEntity::getId).findFirst().orElse(null));
        result.put("answerExplanation", q.getExplanation());
        result.put("recommendation", Map.of("level", p.recommendedLevel(), "explanation", p.explanation(), "source", p.source(), "modelVersion", p.modelVersion()));
        return result;
    }

    public Map<String,Object> progressDto(StudentEntity s) {
        List<ProgressEntity> items = progress.findByStudentIdOrderByUpdatedAtDesc(s.getId()); int total=items.stream().mapToInt(ProgressEntity::getTotalAttempts).sum(); int correct=items.stream().mapToInt(ProgressEntity::getCorrectAttempts).sum();
        Map<String,Object> result = new LinkedHashMap<>(); result.put("studentId", s.getId()); result.put("globalPercent", total == 0 ? 0 : Math.round(correct*100.0/total));
        result.put("correctPercent", total == 0 ? 0 : Math.round(correct*100.0/total)); result.put("activitiesCompleted", items.stream().mapToInt(ProgressEntity::getCompletedActivities).sum());
        result.put("topics", items.stream().map(this::progressItem).toList()); result.put("strongestTopic", items.stream().max(Comparator.comparingInt(this::accuracy)).map(x->x.getTopic().getName()).orElse("Sin datos"));
        result.put("reinforcementTopic", items.stream().min(Comparator.comparingInt(this::accuracy)).map(x->x.getTopic().getName()).orElse("Sin datos")); return result;
    }

    private Map<String,Object> progressItem(ProgressEntity p) { Map<String,Object> m=new LinkedHashMap<>();m.put("topicId",p.getTopic().getId());m.put("topic",p.getTopic().getName());m.put("course",p.getTopic().getCourse().getName());m.put("progressPercent",Math.min(100,p.getTotalAttempts()*10));m.put("accuracyPercent",accuracy(p));m.put("currentLevel",p.getCurrentLevel());m.put("recommendedLevel",p.getRecommendedLevel());m.put("averageTimeSeconds",p.getAverageTimeSeconds());m.put("successStreak",p.getSuccessStreak());m.put("consecutiveErrors",p.getConsecutiveErrors());m.put("totalAttempts",p.getTotalAttempts());m.put("updatedAt",p.getUpdatedAt());return m; }
    private Map<String,Object> attemptDto(AttemptEntity a) { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",a.getId());m.put("clientAttemptId",a.getClientAttemptId());m.put("questionId",a.getQuestion().getId());m.put("prompt",a.getQuestion().getPrompt());m.put("topicId",a.getQuestion().getTopic().getId());m.put("topic",a.getQuestion().getTopic().getName());m.put("course",a.getQuestion().getTopic().getCourse().getName());m.put("correct",a.isCorrect());m.put("responseTimeSeconds",a.getResponseTimeSeconds());m.put("levelBefore",a.getLevelBefore());m.put("recommendedLevel",a.getRecommendedLevel());m.put("aiExplanation",a.getAiExplanation());m.put("recommendationSource",a.getRecommendationSource());m.put("occurredAt",a.getOccurredAt()); return m; }
    private ProgressEntity newProgress(StudentEntity s, TopicEntity t) { ProgressEntity p=new ProgressEntity();p.setStudent(s);p.setTopic(t);return p; }
    private int accuracy(ProgressEntity p) { return p.getTotalAttempts()==0?0:(int)Math.round(p.getCorrectAttempts()*100.0/p.getTotalAttempts()); }
    private int levelIndex(String level) { return switch(level==null?"basico":level){case "avanzado"->2;case "intermedio"->1;default->0;}; }
    private StudentEntity student(String email) { return students.findByUserEmailIgnoreCase(email).orElseThrow(() -> notFound("Estudiante")); }
    private ResponseStatusException notFound(String name) { return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " no encontrado."); }
}
