ALTER TABLE students DROP CONSTRAINT ck_students_grade;
ALTER TABLE classrooms DROP CONSTRAINT ck_classrooms_grade;
ALTER TABLE questions DROP CONSTRAINT ck_questions_grade;

ALTER TABLE students ALTER COLUMN grade INT NOT NULL;
ALTER TABLE classrooms ALTER COLUMN grade INT NOT NULL;
ALTER TABLE questions ALTER COLUMN grade INT NOT NULL;

ALTER TABLE students ADD CONSTRAINT ck_students_grade CHECK (grade BETWEEN 1 AND 5);
ALTER TABLE classrooms ADD CONSTRAINT ck_classrooms_grade CHECK (grade BETWEEN 1 AND 5);
ALTER TABLE questions ADD CONSTRAINT ck_questions_grade CHECK (grade BETWEEN 1 AND 5);
