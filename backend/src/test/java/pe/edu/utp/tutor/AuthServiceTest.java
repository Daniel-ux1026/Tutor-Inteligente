package pe.edu.utp.tutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.tutor.auth.JwtService;
import pe.edu.utp.tutor.domain.*;
import pe.edu.utp.tutor.repository.*;
import pe.edu.utp.tutor.service.AuthService;

class AuthServiceTest {
    @Test
    void rejectsInvalidPasswordAndCountsFailure() {
        UserRepository users=mock(UserRepository.class);RoleRepository roles=mock(RoleRepository.class);StudentRepository students=mock(StudentRepository.class);TeacherRepository teachers=mock(TeacherRepository.class);PasswordRecoveryTokenRepository tokens=mock(PasswordRecoveryTokenRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);JwtService jwt=mock(JwtService.class);
        UserEntity user=new UserEntity();user.setEmail("alumno@demo.pe");user.setPasswordHash("hash");
        when(users.findByEmailIgnoreCase("alumno@demo.pe")).thenReturn(Optional.of(user));when(encoder.matches("mala","hash")).thenReturn(false);
        AuthService service=new AuthService(users,roles,students,teachers,tokens,encoder,jwt,"test-teacher-invitation",true,true);
        assertThatThrownBy(()->service.login(new LoginRequest("alumno@demo.pe","mala"))).isInstanceOf(ResponseStatusException.class);
        verify(users).save(user);
    }

    @Test
    void rejectsTeacherRegistrationWithoutValidInvitation() {
        UserRepository users=mock(UserRepository.class);RoleRepository roles=mock(RoleRepository.class);StudentRepository students=mock(StudentRepository.class);TeacherRepository teachers=mock(TeacherRepository.class);PasswordRecoveryTokenRepository tokens=mock(PasswordRecoveryTokenRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);JwtService jwt=mock(JwtService.class);
        AuthService service=new AuthService(users,roles,students,teachers,tokens,encoder,jwt,"invite-only",true,true);
        RegisterRequest request=new RegisterRequest("Docente","docente@colegio.pe","ClaveSegura2026","TEACHER",null,null,"incorrecto");
        assertThatThrownBy(()->service.register(request)).isInstanceOf(ResponseStatusException.class);
        verify(users,never()).save(any());
    }
}
