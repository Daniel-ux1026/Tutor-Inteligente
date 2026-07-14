CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT ck_roles_name CHECK (name IN ('STUDENT', 'TEACHER', 'ADMIN'))
);

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id BIGINT NOT NULL,
    email NVARCHAR(180) NOT NULL UNIQUE,
    password_hash NVARCHAR(100) NOT NULL,
    full_name NVARCHAR(160) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT df_users_enabled DEFAULT 1,
    failed_attempts INT NOT NULL CONSTRAINT df_users_failed DEFAULT 0,
    locked_until DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_users_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE students (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    grade TINYINT NOT NULL,
    section NVARCHAR(10) NOT NULL,
    CONSTRAINT fk_students_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_students_grade CHECK (grade BETWEEN 1 AND 5)
);

CREATE TABLE teachers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialty NVARCHAR(120) NULL,
    CONSTRAINT fk_teachers_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE classrooms (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    grade TINYINT NOT NULL,
    section NVARCHAR(10) NOT NULL,
    inactivity_days INT NOT NULL CONSTRAINT df_classrooms_inactivity DEFAULT 7,
    CONSTRAINT fk_classrooms_teachers FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT ck_classrooms_grade CHECK (grade BETWEEN 1 AND 5)
);

CREATE TABLE enrollments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    classroom_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME2 NOT NULL CONSTRAINT df_enrollments_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_enrollments UNIQUE (classroom_id, student_id),
    CONSTRAINT fk_enrollments_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_enrollments_students FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE courses (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name NVARCHAR(80) NOT NULL UNIQUE,
    active BIT NOT NULL CONSTRAINT df_courses_active DEFAULT 1
);

CREATE TABLE topics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    course_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL UNIQUE,
    name NVARCHAR(120) NOT NULL,
    display_order INT NOT NULL,
    active BIT NOT NULL CONSTRAINT df_topics_active DEFAULT 1,
    CONSTRAINT uq_topics_course_name UNIQUE (course_id, name),
    CONSTRAINT fk_topics_courses FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE questions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    grade TINYINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    prompt NVARCHAR(1000) NOT NULL,
    explanation NVARCHAR(1000) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_questions_active DEFAULT 1,
    created_by_teacher_id BIGINT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_questions_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_questions_topics FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT fk_questions_teacher FOREIGN KEY (created_by_teacher_id) REFERENCES teachers(id),
    CONSTRAINT ck_questions_grade CHECK (grade BETWEEN 1 AND 5),
    CONSTRAINT ck_questions_difficulty CHECK (difficulty IN ('basico', 'intermedio', 'avanzado'))
);

CREATE TABLE alternatives (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    question_id BIGINT NOT NULL,
    label CHAR(1) NOT NULL,
    text NVARCHAR(500) NOT NULL,
    correct BIT NOT NULL,
    display_order INT NOT NULL,
    CONSTRAINT uq_alternatives_order UNIQUE (question_id, display_order),
    CONSTRAINT fk_alternatives_questions FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE attempts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    client_attempt_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_alternative_id BIGINT NULL,
    correct BIT NOT NULL,
    response_time_seconds INT NOT NULL,
    level_before VARCHAR(20) NOT NULL,
    recommended_level VARCHAR(20) NOT NULL,
    ai_explanation NVARCHAR(1000) NOT NULL,
    recommendation_source VARCHAR(20) NOT NULL,
    occurred_at DATETIME2 NOT NULL,
    received_at DATETIME2 NOT NULL CONSTRAINT df_attempts_received DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_attempts_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attempts_questions FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_attempts_alternatives FOREIGN KEY (selected_alternative_id) REFERENCES alternatives(id),
    CONSTRAINT ck_attempts_time CHECK (response_time_seconds BETWEEN 0 AND 3600)
);

CREATE INDEX ix_attempts_student_time ON attempts(student_id, occurred_at DESC);
CREATE INDEX ix_attempts_question ON attempts(question_id);

CREATE TABLE progress (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    current_level VARCHAR(20) NOT NULL,
    recommended_level VARCHAR(20) NOT NULL,
    total_attempts INT NOT NULL CONSTRAINT df_progress_total DEFAULT 0,
    correct_attempts INT NOT NULL CONSTRAINT df_progress_correct DEFAULT 0,
    average_time_seconds DECIMAL(10,2) NOT NULL CONSTRAINT df_progress_time DEFAULT 0,
    success_streak INT NOT NULL CONSTRAINT df_progress_success DEFAULT 0,
    consecutive_errors INT NOT NULL CONSTRAINT df_progress_errors DEFAULT 0,
    completed_activities INT NOT NULL CONSTRAINT df_progress_activities DEFAULT 0,
    updated_at DATETIME2 NOT NULL CONSTRAINT df_progress_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_progress_student_topic UNIQUE (student_id, topic_id),
    CONSTRAINT fk_progress_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_progress_topics FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE diagnostic_results (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    score_percent DECIMAL(5,2) NOT NULL,
    initial_level VARCHAR(20) NOT NULL,
    answers_json NVARCHAR(MAX) NOT NULL,
    completed_at DATETIME2 NOT NULL CONSTRAINT df_diagnostic_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_diagnostic_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_diagnostic_courses FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE ai_recommendations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    recommended_level VARCHAR(20) NOT NULL,
    explanation NVARCHAR(1000) NOT NULL,
    input_json NVARCHAR(MAX) NOT NULL,
    model_version NVARCHAR(80) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_ai_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_ai_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_ai_topics FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE alerts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    alert_type VARCHAR(40) NOT NULL,
    reason NVARCHAR(600) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    pedagogical_recommendation NVARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_alerts_status DEFAULT 'PENDING',
    created_at DATETIME2 NOT NULL CONSTRAINT df_alerts_created DEFAULT SYSUTCDATETIME(),
    resolved_at DATETIME2 NULL,
    CONSTRAINT fk_alerts_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_alerts_courses FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_alerts_topics FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT ck_alerts_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_alerts_status CHECK (status IN ('PENDING', 'REVIEWED', 'RESOLVED'))
);

CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    attempt_id BIGINT NULL,
    alert_id BIGINT NULL,
    notification_type VARCHAR(40) NOT NULL,
    summary NVARCHAR(600) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    context_json NVARCHAR(MAX) NOT NULL,
    read_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_notifications_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notifications_teachers FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT fk_notifications_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_notifications_courses FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_notifications_topics FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT fk_notifications_attempts FOREIGN KEY (attempt_id) REFERENCES attempts(id),
    CONSTRAINT fk_notifications_alerts FOREIGN KEY (alert_id) REFERENCES alerts(id),
    CONSTRAINT ck_notifications_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX ix_notifications_teacher_unread ON notifications(teacher_id, read_at, created_at DESC);

CREATE TABLE teacher_observations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    text NVARCHAR(2000) NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_observations_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_observations_teachers FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT fk_observations_students FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE assigned_activities (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    instructions NVARCHAR(1500) NOT NULL,
    due_at DATETIME2 NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_activities_status DEFAULT 'ASSIGNED',
    created_at DATETIME2 NOT NULL CONSTRAINT df_activities_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_activities_teachers FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT fk_activities_students FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_activities_topics FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE sync_batches (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    client_batch_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    received_count INT NOT NULL,
    confirmed_count INT NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_sync_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_sync_students FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE password_recovery_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash NVARCHAR(100) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    used_at DATETIME2 NULL,
    CONSTRAINT fk_recovery_users FOREIGN KEY (user_id) REFERENCES users(id)
);
