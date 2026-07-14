package pe.edu.utp.tutor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.tutor.domain.*;
import pe.edu.utp.tutor.repository.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final TeacherRepository teachers;
    private final ObjectMapper mapper;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notifications, TeacherRepository teachers, ObjectMapper mapper) {
        this.notifications = notifications; this.teachers = teachers; this.mapper = mapper;
    }

    public SseEmitter subscribe(String email) {
        TeacherEntity teacher = teachers.findByUserEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado."));
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000);
        emitters.computeIfAbsent(teacher.getId(), ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> emitters.getOrDefault(teacher.getId(), new CopyOnWriteArrayList<>()).remove(emitter);
        emitter.onCompletion(remove); emitter.onTimeout(remove); emitter.onError(error -> remove.run());
        try { emitter.send(SseEmitter.event().name("connected").data(Map.of("unreadCount", notifications.countByTeacherIdAndReadAtIsNull(teacher.getId())))); }
        catch (IOException ex) { emitter.completeWithError(ex); }
        return emitter;
    }

    @Transactional
    public NotificationEntity create(StudentEntity student, TopicEntity topic, AttemptEntity attempt, AlertEntity alert,
                                     String type, String summary, String priority) {
        TeacherEntity teacher = teachers.findAssignedTeacher(student.getId()).orElseGet(() -> teachers.findFirstByOrderByIdAsc().orElseThrow());
        NotificationEntity notification = new NotificationEntity();
        notification.setTeacher(teacher); notification.setStudent(student); notification.setCourse(topic.getCourse()); notification.setTopic(topic);
        notification.setAttempt(attempt); notification.setAlert(alert); notification.setNotificationType(type);
        notification.setSummary(summary); notification.setPriority(priority);
        try {
            notification.setContextJson(mapper.writeValueAsString(Map.of(
                "studentId", student.getId(), "topicId", topic.getId(),
                "attemptId", attempt == null ? "" : attempt.getId(), "alertId", alert == null ? "" : alert.getId())));
        } catch (Exception ex) { notification.setContextJson("{}"); }
        notifications.save(notification); emit(teacher.getId(), dto(notification));
        return notification;
    }

    public Map<String, Object> dto(NotificationEntity n) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", n.getId()); result.put("studentId", n.getStudent().getId()); result.put("studentName", n.getStudent().getUser().getFullName());
        result.put("type", n.getNotificationType()); result.put("course", n.getCourse().getName()); result.put("topic", n.getTopic().getName());
        result.put("topicId", n.getTopic().getId()); result.put("summary", n.getSummary()); result.put("priority", n.getPriority());
        result.put("createdAt", n.getCreatedAt()); result.put("read", n.getReadAt() != null);
        result.put("attemptId", n.getAttempt() == null ? null : n.getAttempt().getId()); result.put("alertId", n.getAlert() == null ? null : n.getAlert().getId());
        return result;
    }

    private void emit(Long teacherId, Object data) {
        for (SseEmitter emitter : emitters.getOrDefault(teacherId, new CopyOnWriteArrayList<>())) {
            try { emitter.send(SseEmitter.event().name("notification").id(UUID.randomUUID().toString()).data(data)); }
            catch (IOException ex) { emitter.complete(); }
        }
    }
}
