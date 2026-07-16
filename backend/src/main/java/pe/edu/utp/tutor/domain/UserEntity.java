package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "role_id") private RoleEntity role;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 160) private String fullName;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "is_demo", nullable = false) private boolean demo;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
