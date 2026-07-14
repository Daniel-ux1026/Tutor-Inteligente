INSERT INTO roles(name) VALUES ('STUDENT'), ('TEACHER'), ('ADMIN');

INSERT INTO courses(code, name) VALUES
('MAT', N'Matemática'),
('COM', N'Comunicación');

DECLARE @mat BIGINT = (SELECT id FROM courses WHERE code = 'MAT');
DECLARE @com BIGINT = (SELECT id FROM courses WHERE code = 'COM');

INSERT INTO topics(course_id, code, name, display_order) VALUES
(@mat, 'MAT_NUM', N'Números y operaciones', 1),
(@mat, 'MAT_ALG', N'Álgebra', 2),
(@mat, 'MAT_GEO', N'Geometría', 3),
(@mat, 'MAT_FUN', N'Funciones', 4),
(@mat, 'MAT_EST', N'Estadística', 5),
(@com, 'COM_COMP', N'Comprensión lectora', 1),
(@com, 'COM_IDEA', N'Idea principal', 2),
(@com, 'COM_INF', N'Inferencias', 3),
(@com, 'COM_VOC', N'Vocabulario', 4),
(@com, 'COM_GRA', N'Gramática y ortografía', 5),
(@com, 'COM_COH', N'Coherencia y cohesión', 6);

CREATE TABLE #seed_questions (
    topic_code VARCHAR(40), grade TINYINT, difficulty VARCHAR(20),
    prompt NVARCHAR(1000), explanation NVARCHAR(1000),
    correct_answer NVARCHAR(500), wrong1 NVARCHAR(500), wrong2 NVARCHAR(500), wrong3 NVARCHAR(500)
);

DECLARE @grade INT = 1;
WHILE @grade <= 5
BEGIN
    INSERT INTO #seed_questions(topic_code, grade, difficulty, prompt, explanation, correct_answer, wrong1, wrong2, wrong3)
    VALUES
    ('MAT_NUM', @grade, 'basico', CONCAT(N'Calcula ', 12 + @grade * 4, N' + ', 8 + @grade * 3, N'.'), N'Suma primero decenas y luego unidades.', CONVERT(NVARCHAR(50), 20 + @grade * 7), CONVERT(NVARCHAR(50), 21 + @grade * 7), CONVERT(NVARCHAR(50), 18 + @grade * 7), CONVERT(NVARCHAR(50), 25 + @grade * 7)),
    ('MAT_NUM', @grade, 'intermedio', CONCAT(N'Resuelve ', @grade + 5, N' × ', @grade + 4, N' - ', @grade + 6, N'.'), N'Respeta el orden de operaciones: multiplicación antes de resta.', CONVERT(NVARCHAR(50), (@grade + 5) * (@grade + 4) - (@grade + 6)), CONVERT(NVARCHAR(50), (@grade + 5) * (@grade + 4)), CONVERT(NVARCHAR(50), (@grade + 4) * (@grade + 4)), CONVERT(NVARCHAR(50), (@grade + 5) * (@grade + 4) - @grade)),
    ('MAT_NUM', @grade, 'avanzado', CONCAT(N'Una familia reparte ', (@grade + 3) * (@grade + 8), N' soles entre ', @grade + 3, N' estudiantes. ¿Cuánto recibe cada uno?'), N'Divide el total entre la cantidad de estudiantes.', CONVERT(NVARCHAR(50), @grade + 8), CONVERT(NVARCHAR(50), @grade + 7), CONVERT(NVARCHAR(50), @grade + 9), CONVERT(NVARCHAR(50), (@grade + 3) * (@grade + 8))),
    ('MAT_ALG', @grade, 'basico', CONCAT(N'Si x + ', @grade + 4, N' = ', @grade * 5 + 13, N', ¿cuánto vale x?'), N'Resta el mismo número a ambos lados.', CONVERT(NVARCHAR(50), @grade * 4 + 9), CONVERT(NVARCHAR(50), @grade * 5 + 13), CONVERT(NVARCHAR(50), @grade + 4), CONVERT(NVARCHAR(50), @grade * 4 + 8)),
    ('MAT_ALG', @grade, 'intermedio', CONCAT(N'Resuelve 2x + ', @grade + 3, N' = ', 3 * @grade + 15, N'.'), N'Resta el término independiente y divide entre 2.', CONVERT(NVARCHAR(50), @grade + 6), CONVERT(NVARCHAR(50), @grade + 5), CONVERT(NVARCHAR(50), 2 * (@grade + 6)), CONVERT(NVARCHAR(50), @grade + 7)),
    ('MAT_ALG', @grade, 'avanzado', CONCAT(N'Resuelve 3(x - ', @grade, N') = ', @grade * 9 + 18, N'.'), N'Divide entre 3 y luego despeja x.', CONVERT(NVARCHAR(50), @grade * 4 + 6), CONVERT(NVARCHAR(50), @grade * 3 + 6), CONVERT(NVARCHAR(50), @grade * 4 + 5), CONVERT(NVARCHAR(50), @grade * 4 + 7)),
    ('MAT_GEO', @grade, 'basico', CONCAT(N'Un rectángulo mide ', @grade + 5, N' cm por ', @grade + 3, N' cm. ¿Cuál es su área?'), N'El área de un rectángulo es largo por ancho.', CONVERT(NVARCHAR(50), (@grade + 5) * (@grade + 3)), CONVERT(NVARCHAR(50), 2 * (@grade + 5 + @grade + 3)), CONVERT(NVARCHAR(50), (@grade + 5) + (@grade + 3)), CONVERT(NVARCHAR(50), (@grade + 4) * (@grade + 3))),
    ('MAT_GEO', @grade, 'intermedio', CONCAT(N'Un triángulo tiene base ', 2 * (@grade + 4), N' cm y altura ', @grade + 3, N' cm. ¿Cuál es su área?'), N'Multiplica base por altura y divide entre 2.', CONVERT(NVARCHAR(50), (@grade + 4) * (@grade + 3)), CONVERT(NVARCHAR(50), 2 * (@grade + 4) * (@grade + 3)), CONVERT(NVARCHAR(50), (@grade + 4) + (@grade + 3)), CONVERT(NVARCHAR(50), (@grade + 5) * (@grade + 3))),
    ('MAT_GEO', @grade, 'avanzado', CONCAT(N'Un prisma mide ', @grade + 2, N' cm, ', @grade + 3, N' cm y ', @grade + 4, N' cm. ¿Cuál es su volumen?'), N'Multiplica las tres dimensiones.', CONVERT(NVARCHAR(50), (@grade + 2) * (@grade + 3) * (@grade + 4)), CONVERT(NVARCHAR(50), (@grade + 2) * (@grade + 3)), CONVERT(NVARCHAR(50), (@grade + 3) * (@grade + 4)), CONVERT(NVARCHAR(50), (@grade + 2) + (@grade + 3) + (@grade + 4))),
    ('MAT_FUN', @grade, 'basico', CONCAT(N'Si f(x) = x + ', @grade + 2, N', calcula f(', @grade + 6, N').'), N'Reemplaza x por el valor indicado.', CONVERT(NVARCHAR(50), 2 * @grade + 8), CONVERT(NVARCHAR(50), @grade + 6), CONVERT(NVARCHAR(50), @grade + 2), CONVERT(NVARCHAR(50), 2 * @grade + 9)),
    ('MAT_FUN', @grade, 'intermedio', CONCAT(N'Si f(x) = 2x - ', @grade + 1, N', calcula f(', @grade + 7, N').'), N'Multiplica por 2 y luego resta.', CONVERT(NVARCHAR(50), @grade + 13), CONVERT(NVARCHAR(50), 2 * (@grade + 7)), CONVERT(NVARCHAR(50), @grade + 12), CONVERT(NVARCHAR(50), @grade + 14)),
    ('MAT_FUN', @grade, 'avanzado', CONCAT(N'Una recta pasa por (0, ', @grade + 2, N') y (2, ', @grade + 8, N'). ¿Cuál es su pendiente?'), N'Pendiente = cambio en y dividido entre cambio en x.', N'3', N'2', N'4', N'6'),
    ('MAT_EST', @grade, 'basico', CONCAT(N'Calcula el promedio de ', @grade + 8, N', ', @grade + 12, N' y ', @grade + 16, N'.'), N'Suma los datos y divide entre tres.', CONVERT(NVARCHAR(50), @grade + 12), CONVERT(NVARCHAR(50), @grade + 11), CONVERT(NVARCHAR(50), @grade + 13), CONVERT(NVARCHAR(50), 3 * @grade + 36)),
    ('MAT_EST', @grade, 'intermedio', CONCAT(N'Ordena ', @grade + 4, N', ', @grade + 12, N', ', @grade + 8, N', ', @grade + 16, N' y ', @grade + 20, N'. ¿Cuál es la mediana?'), N'La mediana es el dato central al ordenar.', CONVERT(NVARCHAR(50), @grade + 12), CONVERT(NVARCHAR(50), @grade + 8), CONVERT(NVARCHAR(50), @grade + 16), CONVERT(NVARCHAR(50), @grade + 10)),
    ('MAT_EST', @grade, 'avanzado', CONCAT(N'Hay ', @grade + 3, N' fichas rojas y ', @grade + 7, N' azules. ¿Cuántas fichas hay en total?'), N'Suma todos los casos posibles.', CONVERT(NVARCHAR(50), 2 * @grade + 10), CONVERT(NVARCHAR(50), @grade + 10), CONVERT(NVARCHAR(50), 2 * @grade + 9), CONVERT(NVARCHAR(50), 2 * @grade + 11));
    SET @grade += 1;
END;

DECLARE @comm TABLE(topic_code VARCHAR(40), base_prompt NVARCHAR(700), explanation NVARCHAR(700), correct_answer NVARCHAR(200), wrong1 NVARCHAR(200), wrong2 NVARCHAR(200), wrong3 NVARCHAR(200));
INSERT INTO @comm VALUES
('COM_COMP', N'Lee: «La comunidad organizó una biblioteca para que niñas y niños accedan a más libros». ¿Qué beneficio se menciona?', N'La respuesta aparece de forma explícita en la oración.', N'Mayor acceso a libros', N'Menos horas de clase', N'Cierre de la escuela', N'Compra de uniformes'),
('COM_IDEA', N'«El agua es esencial para la salud, la agricultura y los ecosistemas; por eso debemos cuidarla». ¿Cuál es la idea principal?', N'Reúne la afirmación que explica todo el texto.', N'El agua es esencial y debe cuidarse', N'La agricultura usa herramientas', N'Los ecosistemas son pequeños', N'La salud depende del deporte'),
('COM_INF', N'Lucía salió con paraguas y botas; al llegar, sacudió gotas de su abrigo. ¿Qué se puede inferir?', N'Las pistas permiten concluir algo que no se dice literalmente.', N'Estaba lloviendo', N'Hacía mucho calor', N'Lucía fue a nadar', N'El abrigo estaba nuevo'),
('COM_VOC', N'En «El científico observó minuciosamente la muestra», ¿qué significa minuciosamente?', N'El contexto indica una observación cuidadosa y detallada.', N'Con mucho detalle', N'Con rapidez', N'Sin interés', N'Desde lejos'),
('COM_GRA', N'Elige la oración escrita correctamente.', N'Revisa mayúscula inicial, concordancia y puntuación.', N'Los estudiantes investigaron el tema.', N'los estudiantes investigó el tema', N'Los estudiante investigaron el tema', N'Los estudiantes investigaron el tema'),
('COM_COH', N'Ordena la secuencia: 1) Finalmente presentó el informe. 2) Primero reunió información. 3) Luego organizó sus hallazgos.', N'Los conectores indican el orden lógico de las acciones.', N'2 - 3 - 1', N'1 - 2 - 3', N'3 - 1 - 2', N'2 - 1 - 3');

DECLARE @levels TABLE(difficulty VARCHAR(20), prefix NVARCHAR(100));
INSERT INTO @levels VALUES ('basico', N'Nivel básico. '), ('intermedio', N'Nivel intermedio. '), ('avanzado', N'Nivel avanzado. ');

INSERT INTO #seed_questions(topic_code, grade, difficulty, prompt, explanation, correct_answer, wrong1, wrong2, wrong3)
SELECT c.topic_code, g.grade, l.difficulty,
       CONCAT(l.prefix, N'Grado ', g.grade, N': ', c.base_prompt),
       c.explanation, c.correct_answer, c.wrong1, c.wrong2, c.wrong3
FROM @comm c
CROSS JOIN (VALUES (1),(2),(3),(4),(5)) g(grade)
CROSS JOIN @levels l;

INSERT INTO questions(topic_id, grade, difficulty, prompt, explanation)
SELECT t.id, s.grade, s.difficulty, s.prompt, s.explanation
FROM #seed_questions s
JOIN topics t ON t.code = s.topic_code;

INSERT INTO alternatives(question_id, label, text, correct, display_order)
SELECT q.id, v.label, v.answer, v.is_correct, v.display_order
FROM #seed_questions s
JOIN topics t ON t.code = s.topic_code
JOIN questions q ON q.topic_id = t.id AND q.grade = s.grade AND q.difficulty = s.difficulty AND q.prompt = s.prompt
CROSS APPLY (VALUES
    ('A', s.correct_answer, CAST(1 AS BIT), 1),
    ('B', s.wrong1, CAST(0 AS BIT), 2),
    ('C', s.wrong2, CAST(0 AS BIT), 3),
    ('D', s.wrong3, CAST(0 AS BIT), 4)
) v(label, answer, is_correct, display_order);

DROP TABLE #seed_questions;
