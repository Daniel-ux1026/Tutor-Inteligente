package pe.edu.utp.tutor.service;

import static pe.edu.utp.tutor.web.dto.ApiDtos.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiClient {
    private final RestClient client;
    public AiClient(RestClient aiRestClient) { this.client = aiRestClient; }

    @SuppressWarnings("unchecked")
    public AiPrediction predict(AiFeatures f) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nivel_actual", f.nivelActual()); payload.put("aciertos_ultimos5", f.aciertosUltimos5());
        payload.put("errores_consecutivos", f.erroresConsecutivos()); payload.put("tiempo_promedio_seg", f.tiempoPromedioSeg());
        payload.put("intentos_tema", f.intentosTema()); payload.put("racha_aciertos", f.rachaAciertos());
        try {
            Map<String, Object> response = client.post().uri("/predict").body(payload).retrieve().body(Map.class);
            if (response == null) throw new IllegalStateException("Respuesta IA vacía");
            return new AiPrediction(String.valueOf(response.get("nivel_recomendado")),
                String.valueOf(response.get("explicacion")), String.valueOf(response.get("version_modelo")), "MODEL",
                (Map<String, Double>) response.getOrDefault("probabilidades", Map.of()));
        } catch (Exception unavailable) {
            return fallback(f);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> metrics() {
        try { return client.get().uri("/metrics").retrieve().body(Map.class); }
        catch (Exception unavailable) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "El servicio de IA no está disponible o el modelo aún no fue entrenado."); }
    }

    private AiPrediction fallback(AiFeatures f) {
        int current = levelIndex(f.nivelActual());
        int target = current;
        if (f.erroresConsecutivos() >= 2 || f.aciertosUltimos5() <= 1 || f.tiempoPromedioSeg() > 150) target--;
        else if (f.aciertosUltimos5() >= 4 && f.erroresConsecutivos() == 0 && f.rachaAciertos() >= 3) target++;
        target = Math.max(0, Math.min(2, target));
        String level = new String[]{"basico", "intermedio", "avanzado"}[target];
        String reason;
        if (target > current) reason = "Sube a " + level + " porque obtuvo " + f.aciertosUltimos5() + " aciertos en sus últimos cinco ejercicios y mantiene una racha de " + f.rachaAciertos() + ".";
        else if (target < current) reason = "Baja a " + level + " para reforzar porque acumula " + f.erroresConsecutivos() + " errores consecutivos y " + f.aciertosUltimos5() + " aciertos recientes.";
        else reason = "Se mantiene en " + level + " porque su desempeño reciente es estable: " + f.aciertosUltimos5() + " aciertos en cinco y " + f.erroresConsecutivos() + " errores consecutivos.";
        return new AiPrediction(level, reason, "rules-fallback-v1", "FALLBACK", Map.of());
    }

    private int levelIndex(String level) {
        return switch (level == null ? "basico" : level) { case "avanzado" -> 2; case "intermedio" -> 1; default -> 0; };
    }
}
