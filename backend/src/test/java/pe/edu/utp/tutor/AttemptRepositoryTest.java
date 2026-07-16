package pe.edu.utp.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.edu.utp.tutor.repository.AttemptRepository;

@DataJpaTest
class AttemptRepositoryTest {
    @Autowired private AttemptRepository attempts;

    @Test
    void evidenceQueryIsValid() {
        assertThat(attempts.findEvidenceAttempts()).isEmpty();
    }
}
