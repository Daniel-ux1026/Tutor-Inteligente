-- Hibernate 6 maps java.time.Instant to SQL Server DATETIMEOFFSET.
-- Preserve the UTC instant semantics used by the API and make schema validation explicit.

DROP INDEX ix_attempts_student_time ON attempts;
DROP INDEX ix_notifications_teacher_unread ON notifications;

ALTER TABLE users DROP CONSTRAINT df_users_created;
ALTER TABLE enrollments DROP CONSTRAINT df_enrollments_created;
ALTER TABLE questions DROP CONSTRAINT df_questions_created;
ALTER TABLE attempts DROP CONSTRAINT df_attempts_received;
ALTER TABLE progress DROP CONSTRAINT df_progress_updated;
ALTER TABLE diagnostic_results DROP CONSTRAINT df_diagnostic_created;
ALTER TABLE ai_recommendations DROP CONSTRAINT df_ai_created;
ALTER TABLE alerts DROP CONSTRAINT df_alerts_created;
ALTER TABLE notifications DROP CONSTRAINT df_notifications_created;
ALTER TABLE teacher_observations DROP CONSTRAINT df_observations_created;
ALTER TABLE assigned_activities DROP CONSTRAINT df_activities_created;
ALTER TABLE sync_batches DROP CONSTRAINT df_sync_created;

ALTER TABLE users ALTER COLUMN locked_until DATETIMEOFFSET(6) NULL;
ALTER TABLE users ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE enrollments ALTER COLUMN enrolled_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE questions ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE attempts ALTER COLUMN occurred_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE attempts ALTER COLUMN received_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE progress ALTER COLUMN updated_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE diagnostic_results ALTER COLUMN completed_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE ai_recommendations ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE alerts ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE alerts ALTER COLUMN resolved_at DATETIMEOFFSET(6) NULL;
ALTER TABLE notifications ALTER COLUMN read_at DATETIMEOFFSET(6) NULL;
ALTER TABLE notifications ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE teacher_observations ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE assigned_activities ALTER COLUMN due_at DATETIMEOFFSET(6) NULL;
ALTER TABLE assigned_activities ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE sync_batches ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE password_recovery_tokens ALTER COLUMN expires_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE password_recovery_tokens ALTER COLUMN used_at DATETIMEOFFSET(6) NULL;

ALTER TABLE users ADD CONSTRAINT df_users_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE enrollments ADD CONSTRAINT df_enrollments_created DEFAULT SYSUTCDATETIME() FOR enrolled_at;
ALTER TABLE questions ADD CONSTRAINT df_questions_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE attempts ADD CONSTRAINT df_attempts_received DEFAULT SYSUTCDATETIME() FOR received_at;
ALTER TABLE progress ADD CONSTRAINT df_progress_updated DEFAULT SYSUTCDATETIME() FOR updated_at;
ALTER TABLE diagnostic_results ADD CONSTRAINT df_diagnostic_created DEFAULT SYSUTCDATETIME() FOR completed_at;
ALTER TABLE ai_recommendations ADD CONSTRAINT df_ai_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE alerts ADD CONSTRAINT df_alerts_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE notifications ADD CONSTRAINT df_notifications_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE teacher_observations ADD CONSTRAINT df_observations_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE assigned_activities ADD CONSTRAINT df_activities_created DEFAULT SYSUTCDATETIME() FOR created_at;
ALTER TABLE sync_batches ADD CONSTRAINT df_sync_created DEFAULT SYSUTCDATETIME() FOR created_at;

CREATE INDEX ix_attempts_student_time ON attempts(student_id, occurred_at DESC);
CREATE INDEX ix_notifications_teacher_unread ON notifications(teacher_id, read_at, created_at DESC);
