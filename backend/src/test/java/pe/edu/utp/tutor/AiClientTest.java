package pe.edu.utp.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import pe.edu.utp.tutor.service.AiClient;

class AiClientTest {
    @Test
    void fallbackDecreasesLevelAfterConsecutiveErrors() {
        AiClient client=new AiClient(RestClient.builder().baseUrl("http://127.0.0.1:1").build());
        AiPrediction prediction=client.predict(new AiFeatures("intermedio",1,2,90,8,0));
        assertThat(prediction.recommendedLevel()).isEqualTo("basico");
        assertThat(prediction.source()).isEqualTo("FALLBACK");
    }
}
