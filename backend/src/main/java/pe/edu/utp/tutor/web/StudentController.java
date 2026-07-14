package pe.edu.utp.tutor.web;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.tutor.service.StudentService;

@RestController @RequestMapping("/api/student")
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service){this.service=service;}
    @GetMapping("/profile") Map<String,Object> profile(Principal p){return service.profile(p.getName());}
    @GetMapping("/courses") List<Map<String,Object>> courses(){return service.courses();}
    @GetMapping("/topics") List<Map<String,Object>> topics(@RequestParam Long courseId){return service.topics(courseId);}
    @PostMapping("/diagnostic") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> diagnostic(Principal p,@Valid @RequestBody DiagnosticRequest r){return service.diagnostic(p.getName(),r);}
    @GetMapping("/diagnostics") List<Map<String,Object>> diagnostics(Principal p){return service.diagnostics(p.getName());}
    @GetMapping("/exercises/next") Map<String,Object> next(Principal p,@RequestParam Long topicId){return service.nextExercise(p.getName(),topicId);}
    @GetMapping("/exercises/offline-pack") List<Map<String,Object>> offlinePack(Principal p,@RequestParam Long topicId,@RequestParam(defaultValue="12") int limit){return service.offlineExercises(p.getName(),topicId,limit);}
    @PostMapping("/attempts") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> attempt(Principal p,@Valid @RequestBody AttemptRequest r){return service.submit(p.getName(),r);}
    @PostMapping("/attempts/sync") SyncResponse sync(Principal p,@Valid @RequestBody SyncRequest r){return service.sync(p.getName(),r);}
    @GetMapping("/progress") Map<String,Object> progress(Principal p){return service.progress(p.getName());}
    @GetMapping("/history") List<Map<String,Object>> history(Principal p){return service.history(p.getName());}
    @GetMapping("/activities") List<Map<String,Object>> activities(Principal p){return service.activities(p.getName());}
    @GetMapping("/notifications") List<Map<String,Object>> notifications(Principal p){return service.notifications(p.getName());}
}
