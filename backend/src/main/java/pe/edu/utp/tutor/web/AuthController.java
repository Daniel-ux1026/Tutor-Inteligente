package pe.edu.utp.tutor.web;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.tutor.service.AuthService;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/refresh") AuthResponse refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request); }
    @PostMapping("/recovery") Map<String, Object> recovery(@Valid @RequestBody RecoveryRequest request) { return auth.recovery(request); }
}
