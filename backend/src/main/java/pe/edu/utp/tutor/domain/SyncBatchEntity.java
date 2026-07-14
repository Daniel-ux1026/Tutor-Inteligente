package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "sync_batches") @Getter @Setter @NoArgsConstructor
public class SyncBatchEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "client_batch_id", nullable = false, unique = true) private UUID clientBatchId;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private StudentEntity student;
    @Column(name = "received_count", nullable = false) private int receivedCount;
    @Column(name = "confirmed_count", nullable = false) private int confirmedCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
