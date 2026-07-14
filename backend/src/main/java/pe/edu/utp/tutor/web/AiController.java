package pe.edu.utp.tutor.web;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.tutor.service.AiClient;

@RestController @RequestMapping("/api/ai")
public class AiController {
    private final AiClient ai;
    public AiController(AiClient ai){this.ai=ai;}
    @PostMapping("/recommendation") AiPrediction recommendation(@Valid @RequestBody AiFeatures features){return ai.predict(features);}
    @GetMapping("/metrics") Map<String,Object> metrics(){return ai.metrics();}
}
