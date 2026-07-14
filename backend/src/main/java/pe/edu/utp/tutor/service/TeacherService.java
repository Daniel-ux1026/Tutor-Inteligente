package pe.edu.utp.tutor.service;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.tutor.domain.*;
import pe.edu.utp.tutor.repository.*;

@Service
public class TeacherService {
    private final TeacherRepository teachers; private final StudentRepository students; private final ProgressRepository progress;
    private final AttemptRepository attempts; private final AlertRepository alerts; private final NotificationRepository notifications;
    private final TeacherObservationRepository observations; private final AssignedActivityRepository activities;
    private final QuestionRepository questions; private final AlternativeRepository alternatives; private final TopicRepository topics; private final CourseRepository courses;
    private final NotificationService notificationService; private final ClassroomRepository classrooms; private final UserRepository users;

    public TeacherService(TeacherRepository teachers, StudentRepository students, ProgressRepository progress, AttemptRepository attempts,
                          AlertRepository alerts, NotificationRepository notifications, TeacherObservationRepository observations,
                          AssignedActivityRepository activities, QuestionRepository questions, AlternativeRepository alternatives,
                          TopicRepository topics, CourseRepository courses, NotificationService notificationService, ClassroomRepository classrooms, UserRepository users) {
        this.teachers=teachers;this.students=students;this.progress=progress;this.attempts=attempts;this.alerts=alerts;
        this.notifications=notifications;this.observations=observations;this.activities=activities;this.questions=questions;
        this.alternatives=alternatives;this.topics=topics;this.courses=courses;this.notificationService=notificationService;this.classrooms=classrooms;this.users=users;
    }

    public Map<String,Object> dashboard(String email) {
        TeacherEntity t=teacher(email); List<StudentEntity> all=students.findAll(); long activeAlerts=alerts.countByStatusNot("RESOLVED");
        long unread=notifications.countByTeacherIdAndReadAtIsNull(t.getId());
        double average=all.stream().flatMap(s->progress.findByStudentIdOrderByUpdatedAtDesc(s.getId()).stream()).mapToInt(this::accuracy).average().orElse(0);
        return Map.of("students",all.size(),"activeAlerts",activeAlerts,"unreadNotifications",unread,"averagePerformance",Math.round(average),"activeQuestions",questions.count(),"assignedActivities",activities.count());
    }

    public List<Map<String,Object>> catalog() {
        return courses.findByActiveTrueOrderByNameAsc().stream().map(course -> {
            Map<String,Object> item=new LinkedHashMap<>();item.put("id",course.getId());item.put("code",course.getCode());item.put("name",course.getName());
            item.put("topics",topics.findByCourseIdAndActiveTrueOrderByDisplayOrderAsc(course.getId()).stream().map(this::topicDto).toList());return item;
        }).toList();
    }

    public List<Map<String,Object>> students(Map<String,String> filters) {
        return students.findAll().stream().map(s->studentRow(s,filters)).filter(Objects::nonNull).toList();
    }

    private Map<String,Object> studentRow(StudentEntity s, Map<String,String> f) {
        if (has(f,"grade") && s.getGrade()!=Integer.parseInt(f.get("grade"))) return null;
        if (has(f,"section") && !s.getSection().equalsIgnoreCase(f.get("section"))) return null;
        List<ProgressEntity> list=progress.findByStudentIdOrderByUpdatedAtDesc(s.getId());
        if (has(f,"course")) list=list.stream().filter(p->p.getTopic().getCourse().getCode().equalsIgnoreCase(f.get("course"))||p.getTopic().getCourse().getName().equalsIgnoreCase(f.get("course"))).toList();
        if (has(f,"topicId")) { long id=Long.parseLong(f.get("topicId")); list=list.stream().filter(p->p.getTopic().getId()==id).toList(); }
        if (has(f,"level")) list=list.stream().filter(p->p.getCurrentLevel().equalsIgnoreCase(f.get("level"))).toList();
        if (has(f,"from")) { Instant from=LocalDate.parse(f.get("from")).atStartOfDay().toInstant(ZoneOffset.UTC); list=list.stream().filter(p->!p.getUpdatedAt().isBefore(from)).toList(); }
        if (has(f,"to")) { Instant to=LocalDate.parse(f.get("to")).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC); list=list.stream().filter(p->p.getUpdatedAt().isBefore(to)).toList(); }
        List<AlertEntity> studentAlerts=alerts.findByStudentIdOrderByCreatedAtDesc(s.getId()).stream().filter(a->!"RESOLVED".equals(a.getStatus())).toList();
        if (has(f,"alertType") && studentAlerts.stream().noneMatch(a->a.getAlertType().equalsIgnoreCase(f.get("alertType")))) return null;
        ProgressEntity focus=list.stream().min(Comparator.comparingInt(this::accuracy)).orElse(null);
        if ((has(f,"course")||has(f,"topicId")||has(f,"level")||has(f,"from")||has(f,"to"))&&focus==null) return null;
        int totalAttempts=list.stream().mapToInt(ProgressEntity::getTotalAttempts).sum();int correctAttempts=list.stream().mapToInt(ProgressEntity::getCorrectAttempts).sum();
        int performance=totalAttempts==0?0:(int)Math.round(correctAttempts*100.0/totalAttempts); String status=performance<60?"AT_RISK":performance>=80?"COMPLETED":"IN_PROGRESS";
        if (has(f,"progressStatus")&&!status.equalsIgnoreCase(f.get("progressStatus"))) return null;
        Map<String,Object> row=new LinkedHashMap<>(); row.put("studentId",s.getId());row.put("studentName",s.getUser().getFullName());row.put("grade",s.getGrade());row.put("section",s.getSection());
        row.put("course",focus==null?"Sin actividad":focus.getTopic().getCourse().getName());row.put("topic",focus==null?"Sin actividad":focus.getTopic().getName());row.put("topicId",focus==null?null:focus.getTopic().getId());
        row.put("performance",performance);row.put("correctAttempts",correctAttempts);row.put("totalAttempts",totalAttempts);row.put("currentLevel",focus==null?"basico":focus.getCurrentLevel());row.put("lastActivity",focus==null?null:focus.getUpdatedAt());row.put("progressStatus",status);
        row.put("alertStatus",studentAlerts.isEmpty()?"Sin alerta":studentAlerts.getFirst().getRiskLevel());row.put("alertType",studentAlerts.isEmpty()?null:studentAlerts.getFirst().getAlertType()); return row;
    }

    public Map<String,Object> studentProgress(Long studentId) {
        StudentEntity s=students.findById(studentId).orElseThrow(()->notFound("Estudiante"));
        List<ProgressEntity> p=progress.findByStudentIdOrderByUpdatedAtDesc(studentId); List<AttemptEntity> recent=attempts.findTop50ByStudentIdOrderByOccurredAtDesc(studentId);
        int total=p.stream().mapToInt(ProgressEntity::getTotalAttempts).sum(), correct=p.stream().mapToInt(ProgressEntity::getCorrectAttempts).sum();
        Map<String,Object> dto=new LinkedHashMap<>();dto.put("student",Map.of("id",s.getId(),"fullName",s.getUser().getFullName(),"email",s.getUser().getEmail(),"grade",s.getGrade(),"section",s.getSection()));
        dto.put("globalProgress",total==0?0:Math.round(correct*100.0/total));dto.put("progressByTopic",p.stream().map(this::progressDto).toList());dto.put("attempts",recent.stream().map(this::attemptDto).toList());
        dto.put("lastFive",recent.stream().limit(5).map(this::attemptDto).toList());dto.put("alerts",alerts.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::alertDto).toList());
        dto.put("observations",observations.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(o->Map.of("id",o.getId(),"text",o.getText(),"createdAt",o.getCreatedAt(),"teacher",o.getTeacher().getUser().getFullName())).toList());
        dto.put("activities",activities.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::activityDto).toList());
        dto.put("syncStatus",Map.of("status","SYNCHRONIZED","lastReceivedAt",recent.isEmpty()?"":recent.getFirst().getReceivedAt(),"pending",0));
        dto.put("pedagogicalRecommendation",alerts.findByStudentIdOrderByCreatedAtDesc(studentId).stream().filter(a->!"RESOLVED".equals(a.getStatus())).findFirst().map(AlertEntity::getPedagogicalRecommendation).orElse("Mantener la práctica diversificada y revisar la evolución semanal.")); return dto;
    }

    public List<Map<String,Object>> alerts(Map<String,String> f) {
        return alerts.findAllByOrderByCreatedAtDesc().stream()
            .filter(a->!has(f,"status")||a.getStatus().equalsIgnoreCase(f.get("status")))
            .filter(a->!has(f,"type")||a.getAlertType().equalsIgnoreCase(f.get("type")))
            .filter(a->!has(f,"risk")||a.getRiskLevel().equalsIgnoreCase(f.get("risk")))
            .filter(a->!has(f,"course")||a.getCourse().getCode().equalsIgnoreCase(f.get("course"))||a.getCourse().getName().equalsIgnoreCase(f.get("course")))
            .filter(a->!has(f,"studentId")||a.getStudent().getId().equals(Long.parseLong(f.get("studentId"))))
            .map(this::alertDto).toList();
    }

    @Transactional public Map<String,Object> updateAlert(Long id, AlertStatusRequest request) {
        AlertEntity a=alerts.findById(id).orElseThrow(()->notFound("Alerta"));a.setStatus(request.status());if("RESOLVED".equals(request.status()))a.setResolvedAt(Instant.now());return alertDto(alerts.save(a));
    }

    public Map<String,Object> notifications(String email, Map<String,String> f) {
        TeacherEntity t=teacher(email);List<NotificationEntity> list=notifications.findTop50ByTeacherIdOrderByCreatedAtDesc(t.getId()).stream()
            .filter(n->!has(f,"type")||n.getNotificationType().equalsIgnoreCase(f.get("type")))
            .filter(n->!has(f,"studentId")||n.getStudent().getId().equals(Long.parseLong(f.get("studentId"))))
            .filter(n->!has(f,"state")||("UNREAD".equalsIgnoreCase(f.get("state"))?n.getReadAt()==null:n.getReadAt()!=null)).toList();
        return Map.of("unreadCount",notifications.countByTeacherIdAndReadAtIsNull(t.getId()),"items",list.stream().map(notificationService::dto).toList());
    }

    @Transactional public Map<String,Object> readNotification(String email,Long id) {
        TeacherEntity t=teacher(email);NotificationEntity n=notifications.findById(id).orElseThrow(()->notFound("Notificación"));
        if(!n.getTeacher().getId().equals(t.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La notificación no pertenece al docente.");
        if(n.getReadAt()==null){n.setReadAt(Instant.now());notifications.save(n);}return notificationService.dto(n);
    }

    public List<Map<String,Object>> questions() { return questions.findAllByOrderByCreatedAtDesc().stream().map(this::questionDto).toList(); }

    @Transactional public Map<String,Object> saveQuestion(String email,Long id,QuestionRequest request) {
        if(request.alternatives().stream().filter(AlternativeRequest::correct).count()!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Debe existir exactamente una alternativa correcta.");
        QuestionEntity q=id==null?new QuestionEntity():questions.findById(id).orElseThrow(()->notFound("Pregunta"));
        q.setTopic(topics.findById(request.topicId()).orElseThrow(()->notFound("Tema")));q.setGrade(request.grade());q.setDifficulty(request.difficulty());q.setPrompt(request.prompt());q.setExplanation(request.explanation());
        if(id==null)q.setCreatedByTeacher(teacher(email));q.getAlternatives().clear();
        int order=1;for(AlternativeRequest item:request.alternatives()){AlternativeEntity a=new AlternativeEntity();a.setQuestion(q);a.setLabel(item.label());a.setText(item.text());a.setCorrect(item.correct());a.setDisplayOrder(order++);q.getAlternatives().add(a);}return questionDto(questions.save(q));
    }

    @Transactional public Map<String,Object> statusQuestion(Long id,QuestionStatusRequest request){QuestionEntity q=questions.findById(id).orElseThrow(()->notFound("Pregunta"));q.setActive(request.active());return questionDto(questions.save(q));}
    @Transactional public Map<String,Object> observation(String email,Long studentId,ObservationRequest request){TeacherObservationEntity o=new TeacherObservationEntity();o.setTeacher(teacher(email));o.setStudent(students.findById(studentId).orElseThrow(()->notFound("Estudiante")));o.setText(request.text());o=observations.save(o);return Map.of("id",o.getId(),"text",o.getText(),"createdAt",o.getCreatedAt());}
    @Transactional public Map<String,Object> activity(String email,ActivityRequest request){AssignedActivityEntity a=new AssignedActivityEntity();a.setTeacher(teacher(email));a.setStudent(students.findById(request.studentId()).orElseThrow(()->notFound("Estudiante")));a.setTopic(topics.findById(request.topicId()).orElseThrow(()->notFound("Tema")));a.setTitle(request.title());a.setInstructions(request.instructions());a.setDueAt(request.dueAt());return activityDto(activities.save(a));}
    @Transactional public Map<String,Object> generateTopicAndAssign(String email,TopicAssignmentRequest request){
        TeacherEntity teacher=teacher(email);StudentEntity student=students.findById(request.studentId()).orElseThrow(()->notFound("Estudiante"));CourseEntity course=courses.findById(request.courseId()).orElseThrow(()->notFound("Curso"));
        String name=request.topicName().trim();TopicEntity topic=topics.findByCourseIdAndNameIgnoreCase(course.getId(),name).orElse(null);boolean created=false;
        if(topic==null){topic=new TopicEntity();topic.setCourse(course);topic.setName(name);topic.setCode(course.getCode()+"_GEN_"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));topic.setDisplayOrder(topics.findByCourseIdAndActiveTrueOrderByDisplayOrderAsc(course.getId()).size()+1);topic=topics.save(topic);createStarterExercises(teacher,topic);created=true;}
        AssignedActivityEntity activity=new AssignedActivityEntity();activity.setTeacher(teacher);activity.setStudent(student);activity.setTopic(topic);activity.setTitle(request.title());activity.setInstructions(request.instructions());activity.setDueAt(request.dueAt());activity=activities.save(activity);
        Map<String,Object> result=new LinkedHashMap<>();result.put("created",created);result.put("topic",topicDto(topic));result.put("exerciseCount",questions.countByTopicId(topic.getId()));result.put("activity",activityDto(activity));return result;
    }
    public List<Map<String,Object>> activities(){return activities.findAllByOrderByCreatedAtDesc().stream().map(this::activityDto).toList();}

    private void createStarterExercises(TeacherEntity teacher,TopicEntity topic){
        String[] levels={"basico","intermedio","avanzado"};
        for(int grade=1;grade<=5;grade++)for(int level=0;level<levels.length;level++)for(int variant=1;variant<=2;variant++){
            QuestionEntity question=new QuestionEntity();question.setTopic(topic);question.setGrade(grade);question.setDifficulty(levels[level]);question.setCreatedByTeacher(teacher);
            List<String> answers;
            if("MAT".equalsIgnoreCase(topic.getCourse().getCode())){
                int left=grade*7+(level+1)*4+variant;int right=grade+level+variant+2;int correct=left+right;
                question.setPrompt("Práctica de "+topic.getName()+": calcula "+left+" + "+right+".");question.setExplanation("Suma las unidades y luego las decenas para comprobar el resultado.");
                answers=List.of(String.valueOf(correct),String.valueOf(correct-1),String.valueOf(correct+2),String.valueOf(correct+5));
            }else{
                question.setPrompt("Grado "+grade+", ejercicio "+variant+" de "+topic.getName()+": lee «El grupo estudió el tema, comparó ideas y explicó sus conclusiones». ¿Cuál es la acción principal?");question.setExplanation("La acción central reúne lo que realizó el grupo durante la actividad.");
                answers=List.of("Estudió, comparó y explicó sus conclusiones","Copió una sola palabra","Evitó revisar el tema","Terminó sin comunicar resultados");
            }
            String[] labels={"A","B","C","D"};for(int index=0;index<labels.length;index++){AlternativeEntity alternative=new AlternativeEntity();alternative.setQuestion(question);alternative.setLabel(labels[index]);alternative.setText(answers.get(index));alternative.setCorrect(index==0);alternative.setDisplayOrder(index+1);question.getAlternatives().add(alternative);}questions.save(question);
        }
    }

    public Map<String,Object> settings(String email){TeacherEntity t=teacher(email);List<ClassroomEntity> rooms=classrooms.findByTeacherIdOrderByGradeAsc(t.getId());return Map.of("fullName",t.getUser().getFullName(),"email",t.getUser().getEmail(),"specialty",t.getSpecialty()==null?"":t.getSpecialty(),"inactivityDays",rooms.isEmpty()?7:rooms.getFirst().getInactivityDays(),"classrooms",rooms.stream().map(c->Map.of("id",c.getId(),"name",c.getName(),"grade",c.getGrade(),"section",c.getSection())).toList());}
    @Transactional public Map<String,Object> saveSettings(String email,ClassroomSettingsRequest request){TeacherEntity t=teacher(email);t.getUser().setFullName(request.fullName().trim());users.save(t.getUser());for(ClassroomEntity c:classrooms.findByTeacherIdOrderByGradeAsc(t.getId())){c.setInactivityDays(request.inactivityDays());classrooms.save(c);}return settings(email);}

    private Map<String,Object> progressDto(ProgressEntity p){Map<String,Object>m=new LinkedHashMap<>();m.put("topicId",p.getTopic().getId());m.put("topic",p.getTopic().getName());m.put("course",p.getTopic().getCourse().getName());m.put("accuracyPercent",accuracy(p));m.put("progressPercent",Math.min(100,p.getTotalAttempts()*10));m.put("currentLevel",p.getCurrentLevel());m.put("recommendedLevel",p.getRecommendedLevel());m.put("averageTimeSeconds",p.getAverageTimeSeconds());m.put("successStreak",p.getSuccessStreak());m.put("consecutiveErrors",p.getConsecutiveErrors());m.put("totalAttempts",p.getTotalAttempts());m.put("updatedAt",p.getUpdatedAt());return m;}
    private Map<String,Object> attemptDto(AttemptEntity a){Map<String,Object>m=new LinkedHashMap<>();m.put("id",a.getId());m.put("questionId",a.getQuestion().getId());m.put("prompt",a.getQuestion().getPrompt());m.put("topicId",a.getQuestion().getTopic().getId());m.put("topic",a.getQuestion().getTopic().getName());m.put("course",a.getQuestion().getTopic().getCourse().getName());m.put("correct",a.isCorrect());m.put("responseTimeSeconds",a.getResponseTimeSeconds());m.put("levelBefore",a.getLevelBefore());m.put("recommendedLevel",a.getRecommendedLevel());m.put("aiExplanation",a.getAiExplanation());m.put("occurredAt",a.getOccurredAt());return m;}
    private Map<String,Object> alertDto(AlertEntity a){Map<String,Object>m=new LinkedHashMap<>();m.put("id",a.getId());m.put("studentId",a.getStudent().getId());m.put("studentName",a.getStudent().getUser().getFullName());m.put("course",a.getCourse().getName());m.put("topicId",a.getTopic().getId());m.put("topic",a.getTopic().getName());m.put("type",a.getAlertType());m.put("reason",a.getReason());m.put("riskLevel",a.getRiskLevel());m.put("pedagogicalRecommendation",a.getPedagogicalRecommendation());m.put("status",a.getStatus());m.put("createdAt",a.getCreatedAt());return m;}
    private Map<String,Object> questionDto(QuestionEntity q){Map<String,Object>m=new LinkedHashMap<>();m.put("id",q.getId());m.put("topicId",q.getTopic().getId());m.put("topic",q.getTopic().getName());m.put("course",q.getTopic().getCourse().getName());m.put("grade",q.getGrade());m.put("difficulty",q.getDifficulty());m.put("prompt",q.getPrompt());m.put("explanation",q.getExplanation());m.put("active",q.isActive());m.put("createdAt",q.getCreatedAt());m.put("alternatives",q.getAlternatives().stream().map(a->Map.of("id",a.getId(),"label",a.getLabel(),"text",a.getText(),"correct",a.isCorrect())).toList());return m;}
    private Map<String,Object> activityDto(AssignedActivityEntity a){return Map.of("id",a.getId(),"studentId",a.getStudent().getId(),"studentName",a.getStudent().getUser().getFullName(),"topicId",a.getTopic().getId(),"topic",a.getTopic().getName(),"title",a.getTitle(),"instructions",a.getInstructions(),"dueAt",a.getDueAt()==null?"":a.getDueAt(),"status",a.getStatus(),"createdAt",a.getCreatedAt());}
    private Map<String,Object> topicDto(TopicEntity t){return Map.of("id",t.getId(),"code",t.getCode(),"name",t.getName(),"courseId",t.getCourse().getId(),"course",t.getCourse().getName());}
    private int accuracy(ProgressEntity p){return p.getTotalAttempts()==0?0:(int)Math.round(p.getCorrectAttempts()*100.0/p.getTotalAttempts());}
    private boolean has(Map<String,String>m,String key){return m.get(key)!=null&&!m.get(key).isBlank();}
    private TeacherEntity teacher(String email){return teachers.findByUserEmailIgnoreCase(email).orElseThrow(()->notFound("Docente"));}
    private ResponseStatusException notFound(String name){return new ResponseStatusException(HttpStatus.NOT_FOUND,name+" no encontrado.");}
}
