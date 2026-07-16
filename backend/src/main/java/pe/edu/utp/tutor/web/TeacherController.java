package pe.edu.utp.tutor.web;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.utp.tutor.service.NotificationService;
import pe.edu.utp.tutor.service.LearningEffectivenessService;
import pe.edu.utp.tutor.service.TeacherService;

@RestController @RequestMapping("/api/teacher")
public class TeacherController {
    private final TeacherService service;private final NotificationService notificationService;private final LearningEffectivenessService effectivenessService;
    public TeacherController(TeacherService service,NotificationService notificationService,LearningEffectivenessService effectivenessService){this.service=service;this.notificationService=notificationService;this.effectivenessService=effectivenessService;}
    @GetMapping("/dashboard") Map<String,Object> dashboard(Principal p){return service.dashboard(p.getName());}
    @GetMapping("/catalog") List<Map<String,Object>> catalog(){return service.catalog();}
    @GetMapping("/students") List<Map<String,Object>> students(@RequestParam Map<String,String> filters){return service.students(filters);}
    @GetMapping("/students/{id}/progress") Map<String,Object> progress(@PathVariable Long id){return service.studentProgress(id);}
    @PostMapping("/students/{id}/observations") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> observation(Principal p,@PathVariable Long id,@Valid @RequestBody ObservationRequest r){return service.observation(p.getName(),id,r);}
    @GetMapping("/alerts") List<Map<String,Object>> alerts(@RequestParam Map<String,String> filters){return service.alerts(filters);}
    @PatchMapping("/alerts/{id}") Map<String,Object> alert(@PathVariable Long id,@Valid @RequestBody AlertStatusRequest r){return service.updateAlert(id,r);}
    @GetMapping("/notifications") Map<String,Object> notifications(Principal p,@RequestParam Map<String,String> filters){return service.notifications(p.getName(),filters);}
    @PutMapping("/notifications/{id}/read") Map<String,Object> read(Principal p,@PathVariable Long id){return service.readNotification(p.getName(),id);}
    @GetMapping(value="/notifications/stream",produces="text/event-stream") SseEmitter stream(Principal p){return notificationService.subscribe(p.getName());}
    @GetMapping("/questions") List<Map<String,Object>> questions(){return service.questions();}
    @PostMapping("/questions") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> question(Principal p,@Valid @RequestBody QuestionRequest r){return service.saveQuestion(p.getName(),null,r);}
    @PutMapping("/questions/{id}") Map<String,Object> updateQuestion(Principal p,@PathVariable Long id,@Valid @RequestBody QuestionRequest r){return service.saveQuestion(p.getName(),id,r);}
    @PatchMapping("/questions/{id}/status") Map<String,Object> questionStatus(@PathVariable Long id,@RequestBody QuestionStatusRequest r){return service.statusQuestion(id,r);}
    @GetMapping("/activities") List<Map<String,Object>> activities(){return service.activities();}
    @PostMapping("/activities") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> activity(Principal p,@Valid @RequestBody ActivityRequest r){return service.activity(p.getName(),r);}
    @PostMapping("/topics/assign") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> topicAssignment(Principal p,@Valid @RequestBody TopicAssignmentRequest r){return service.generateTopicAndAssign(p.getName(),r);}
    @GetMapping("/reports/learning-effectiveness") Map<String,Object> learningEffectiveness(){return effectivenessService.report();}
    @GetMapping("/settings") Map<String,Object> settings(Principal p){return service.settings(p.getName());}
    @PatchMapping("/settings") Map<String,Object> settings(Principal p,@Valid @RequestBody ClassroomSettingsRequest r){return service.saveSettings(p.getName(),r);}
}
