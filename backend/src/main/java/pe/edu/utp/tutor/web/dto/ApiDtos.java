package pe.edu.utp.tutor.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() { }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record RegisterRequest(@NotBlank @Size(max = 160) String fullName,
                                  @Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @Pattern(regexp = "STUDENT|TEACHER") String role,
                                  @Min(1) @Max(5) Integer grade,
                                  @Size(max = 10) String section,
                                  @Size(max = 128) String teacherInvitationCode) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record RecoveryRequest(@Email @NotBlank String email, String code,
                                  @Size(min = 8, max = 72) String newPassword) { }
    public record UserSummary(Long id, String email, String fullName, String role,
                              Long profileId, Integer grade, String section) { }
    public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserSummary user) { }

    public record DiagnosticRequest(@NotNull Long courseId,
                                    @DecimalMin("0") @DecimalMax("100") double scorePercent,
                                    @NotNull Map<String, Object> answers) { }

    public record AttemptRequest(@NotNull UUID clientAttemptId,
                                 @NotNull Long questionId,
                                 @NotNull Long selectedAlternativeId,
                                 @Min(0) @Max(3600) int responseTimeSeconds,
                                 @NotNull Instant occurredAt) { }
    public record SyncRequest(@NotNull UUID clientBatchId,
                              @NotEmpty List<@Valid AttemptRequest> attempts) { }
    public record SyncResponse(UUID clientBatchId, List<UUID> confirmedClientAttemptIds,
                               List<UUID> duplicatedClientAttemptIds, Instant synchronizedAt) { }

    public record QuestionRequest(@NotNull Long topicId, @Min(1) @Max(5) int grade,
                                  @Pattern(regexp = "basico|intermedio|avanzado") String difficulty,
                                  @NotBlank @Size(max = 1000) String prompt,
                                  @NotBlank @Size(max = 1000) String explanation,
                                  @Size(min = 4, max = 4) List<@Valid AlternativeRequest> alternatives) { }
    public record AlternativeRequest(@NotBlank @Size(max = 1) String label,
                                     @NotBlank @Size(max = 500) String text,
                                     boolean correct) { }
    public record QuestionStatusRequest(boolean active) { }
    public record ObservationRequest(@NotBlank @Size(max = 2000) String text) { }
    public record ActivityRequest(@NotNull Long studentId, @NotNull Long topicId,
                                  @NotBlank @Size(max = 200) String title,
                                  @NotBlank @Size(max = 1500) String instructions,
                                  Instant dueAt) { }
    public record TopicAssignmentRequest(@NotNull Long courseId, @NotNull Long studentId,
                                         @NotBlank @Size(max = 120) String topicName,
                                         @NotBlank @Size(max = 200) String title,
                                         @NotBlank @Size(max = 1500) String instructions,
                                         Instant dueAt) { }
    public record AlertStatusRequest(@Pattern(regexp = "PENDING|REVIEWED|RESOLVED") String status) { }
    public record ClassroomSettingsRequest(@NotBlank @Size(max = 160) String fullName,
                                           @Min(1) @Max(60) int inactivityDays) { }

    public record AiFeatures(@Pattern(regexp = "basico|intermedio|avanzado") String nivelActual,
                             @Min(0) @Max(5) int aciertosUltimos5,
                             @Min(0) int erroresConsecutivos,
                             @Min(0) double tiempoPromedioSeg,
                             @Min(0) int intentosTema,
                             @Min(0) int rachaAciertos) { }
    public record AiPrediction(String recommendedLevel, String explanation, String modelVersion,
                               String source, Map<String, Double> probabilities) { }
}
