package pe.edu.utp.tutor.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient aiRestClient(@Value("${app.ai.base-url}") String baseUrl,
                            @Value("${app.ai.timeout-ms}") int timeoutMs,
                            @Value("${app.ai.api-key:}") String apiKey) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        var builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-Internal-API-Key", apiKey.trim());
        }
        return builder.build();
    }
}
