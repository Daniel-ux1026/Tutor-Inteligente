package pe.edu.utp.tutor.service;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.tutor.auth.JwtService;
import pe.edu.utp.tutor.domain.*;
import pe.edu.utp.tutor.repository.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final PasswordRecoveryTokenRepository recoveryTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String teacherInvitationCode;
    private final boolean recoveryEnabled;
    private final boolean exposeLocalRecoveryCode;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, RoleRepository roles, StudentRepository students, TeacherRepository teachers,
                       PasswordRecoveryTokenRepository recoveryTokens, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.registration.teacher-invitation-code:}") String teacherInvitationCode,
                       @Value("${app.recovery.enabled:true}") boolean recoveryEnabled,
                       @Value("${app.recovery.expose-local-code:true}") boolean exposeLocalRecoveryCode) {
        this.users = users; this.roles = roles; this.students = students; this.teachers = teachers;
        this.recoveryTokens = recoveryTokens; this.encoder = encoder; this.jwt = jwt;
        this.teacherInvitationCode = teacherInvitationCode == null ? "" : teacherInvitationCode.trim();
        this.recoveryEnabled = recoveryEnabled;
        this.exposeLocalRecoveryCode = exposeLocalRecoveryCode;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = users.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."));
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Cuenta bloqueada temporalmente. Intenta nuevamente en cinco minutos.");
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setFailedAttempts(0);
                user.setLockedUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
            }
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }
        user.setFailedAttempts(0); user.setLockedUntil(null); users.save(user);
        return response(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        String roleName = request.role() == null ? "STUDENT" : request.role();
        if ("TEACHER".equals(roleName) && !validTeacherInvitation(request.teacherInvitationCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El código de invitación docente no es válido.");
        }
        RoleEntity role = roles.findByName(roleName).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase()); user.setFullName(request.fullName().trim());
        user.setPasswordHash(encoder.encode(request.password())); user.setRole(role);
        users.save(user);
        if ("STUDENT".equals(roleName)) {
            if (request.grade() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grado es obligatorio para estudiantes.");
            StudentEntity student = new StudentEntity(); student.setUser(user); student.setGrade(request.grade());
            student.setSection(request.section() == null || request.section().isBlank() ? "A" : request.section().trim()); students.save(student);
        } else {
            TeacherEntity teacher = new TeacherEntity(); teacher.setUser(user); teacher.setSpecialty("Educación secundaria"); teachers.save(teacher);
        }
        return response(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try { claims = jwt.parse(request.refreshToken()); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado."); }
        if (!"refresh".equals(claims.get("type", String.class))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token incorrecto.");
        UserEntity user = users.findByEmailIgnoreCase(claims.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        return response(user);
    }

    @Transactional
    public Map<String, Object> recovery(RecoveryRequest request) {
        String genericMessage = "Si la cuenta existe y la recuperación está habilitada, recibirás instrucciones para continuar.";
        if (!recoveryEnabled) {
            if (request.code() != null && !request.code().isBlank()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La recuperación de contraseña no está habilitada.");
            }
            return Map.of("message", genericMessage);
        }
        var foundUser = users.findByEmailIgnoreCase(request.email().trim());
        if (request.code() == null || request.code().isBlank()) {
            if (foundUser.isEmpty()) return Map.of("message", genericMessage);
            UserEntity user = foundUser.get();
            String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
            PasswordRecoveryTokenEntity token = new PasswordRecoveryTokenEntity(); token.setUser(user);
            token.setCodeHash(encoder.encode(code)); token.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
            recoveryTokens.save(token);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", exposeLocalRecoveryCode
                ? "Código local generado para el entorno de desarrollo."
                : genericMessage);
            if (exposeLocalRecoveryCode) response.put("demoCode", code);
            response.put("expiresInMinutes", 15);
            return response;
        }
        if (request.newPassword() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa la nueva contraseña.");
        UserEntity user = foundUser.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o vencido."));
        PasswordRecoveryTokenEntity token = recoveryTokens.findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(user.getId()).stream()
            .filter(t -> t.getExpiresAt().isAfter(Instant.now()) && encoder.matches(request.code(), t.getCodeHash())).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o vencido."));
        user.setPasswordHash(encoder.encode(request.newPassword())); users.save(user); token.setUsedAt(Instant.now()); recoveryTokens.save(token);
        return Map.of("message", "Contraseña actualizada correctamente.");
    }

    private boolean validTeacherInvitation(String supplied) {
        if (teacherInvitationCode.isBlank() || supplied == null || supplied.isBlank()) return false;
        return MessageDigest.isEqual(
            teacherInvitationCode.getBytes(StandardCharsets.UTF_8),
            supplied.trim().getBytes(StandardCharsets.UTF_8));
    }

    private AuthResponse response(UserEntity user) {
        Long profileId = null; Integer grade = null; String section = null;
        if ("STUDENT".equals(user.getRole().getName())) {
            StudentEntity student = students.findByUserEmailIgnoreCase(user.getEmail()).orElseThrow();
            profileId = student.getId(); grade = student.getGrade(); section = student.getSection();
        } else {
            profileId = teachers.findByUserEmailIgnoreCase(user.getEmail()).orElseThrow().getId();
        }
        return new AuthResponse(jwt.accessToken(user), jwt.refreshToken(user), jwt.accessSeconds(),
            new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole().getName(), profileId, grade, section));
    }
}
