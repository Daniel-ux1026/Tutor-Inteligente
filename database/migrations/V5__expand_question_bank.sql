CREATE TABLE #extra_questions (
    topic_id BIGINT NOT NULL,
    grade INT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    prompt NVARCHAR(1000) NOT NULL,
    explanation NVARCHAR(1000) NOT NULL,
    correct_answer NVARCHAR(500) NOT NULL,
    wrong1 NVARCHAR(500) NOT NULL,
    wrong2 NVARCHAR(500) NOT NULL,
    wrong3 NVARCHAR(500) NOT NULL
);

-- Dos variaciones adicionales por grado y dificultad para cada tema de Matemática.
INSERT INTO #extra_questions(topic_id, grade, difficulty, prompt, explanation, correct_answer, wrong1, wrong2, wrong3)
SELECT t.id, g.grade, l.difficulty,
    CASE t.code
        WHEN 'MAT_NUM' THEN CONCAT(N'Variación ', v.variant, N'. Calcula ', n.a, N' + ', n.b, N'.')
        WHEN 'MAT_ALG' THEN CONCAT(N'Variación ', v.variant, N'. Si x + ', n.b, N' = ', n.a + n.b, N', ¿cuánto vale x?')
        WHEN 'MAT_GEO' THEN CONCAT(N'Variación ', v.variant, N'. Un rectángulo mide ', n.a, N' cm por ', n.b, N' cm. ¿Cuál es su área?')
        WHEN 'MAT_FUN' THEN CONCAT(N'Variación ', v.variant, N'. Si f(x) = 2x + ', n.b, N', calcula f(', n.a, N').')
        ELSE CONCAT(N'Variación ', v.variant, N'. Calcula el promedio de ', n.a, N', ', n.a + 3, N' y ', n.a + 6, N'.')
    END,
    CASE t.code
        WHEN 'MAT_NUM' THEN N'Suma las unidades y luego las decenas.'
        WHEN 'MAT_ALG' THEN N'Resta el mismo término en ambos lados para despejar x.'
        WHEN 'MAT_GEO' THEN N'El área del rectángulo se obtiene multiplicando largo por ancho.'
        WHEN 'MAT_FUN' THEN N'Reemplaza x por el valor indicado, multiplica por 2 y suma el término independiente.'
        ELSE N'Suma los tres datos y divide el resultado entre tres.'
    END,
    CONVERT(NVARCHAR(500), CASE t.code
        WHEN 'MAT_NUM' THEN n.a + n.b
        WHEN 'MAT_ALG' THEN n.a
        WHEN 'MAT_GEO' THEN n.a * n.b
        WHEN 'MAT_FUN' THEN 2 * n.a + n.b
        ELSE n.a + 3
    END),
    CONVERT(NVARCHAR(500), CASE t.code
        WHEN 'MAT_NUM' THEN n.a + n.b - 1
        WHEN 'MAT_ALG' THEN n.a - 1
        WHEN 'MAT_GEO' THEN n.a + n.b
        WHEN 'MAT_FUN' THEN 2 * n.a
        ELSE n.a
    END),
    CONVERT(NVARCHAR(500), CASE t.code
        WHEN 'MAT_NUM' THEN n.a + n.b + 2
        WHEN 'MAT_ALG' THEN n.a + 2
        WHEN 'MAT_GEO' THEN 2 * (n.a + n.b)
        WHEN 'MAT_FUN' THEN 2 * n.a + n.b + 2
        ELSE n.a + 6
    END),
    CONVERT(NVARCHAR(500), CASE t.code
        WHEN 'MAT_NUM' THEN n.a - n.b
        WHEN 'MAT_ALG' THEN n.a + n.b
        WHEN 'MAT_GEO' THEN n.a * n.b + n.b
        WHEN 'MAT_FUN' THEN n.a + n.b
        ELSE n.a + 2
    END)
FROM topics t
JOIN courses c ON c.id = t.course_id AND c.code = 'MAT'
CROSS JOIN (VALUES (1),(2),(3),(4),(5)) g(grade)
CROSS JOIN (VALUES ('basico',1),('intermedio',2),('avanzado',3)) l(difficulty, level_rank)
CROSS JOIN (VALUES (1),(2)) v(variant)
CROSS APPLY (SELECT g.grade * 4 + l.level_rank * 3 + v.variant + 4 AS a,
                    g.grade + l.level_rank + v.variant + 2 AS b) n;

DECLARE @communication TABLE (
    topic_code VARCHAR(40), variant INT, prompt NVARCHAR(700), explanation NVARCHAR(700),
    correct_answer NVARCHAR(200), wrong1 NVARCHAR(200), wrong2 NVARCHAR(200), wrong3 NVARCHAR(200)
);

INSERT INTO @communication VALUES
('COM_COMP',1,N'Lee: «La escuela abrió un club de lectura los viernes y cada estudiante puede elegir un libro». ¿Qué actividad ofrece la escuela?',N'La respuesta está expresada directamente en el texto.',N'Un club de lectura',N'Un torneo deportivo',N'Una feria de ciencias',N'Un curso de pintura'),
('COM_COMP',2,N'Lee: «El municipio colocó contenedores para separar papel, vidrio y plástico». ¿Qué acción realizó el municipio?',N'Identifica la información explícita de la oración.',N'Colocó contenedores de reciclaje',N'Cerró los parques',N'Construyó una biblioteca',N'Organizó un concierto'),
('COM_IDEA',1,N'«Dormir bien mejora la concentración, el ánimo y la salud; por eso es importante respetar horarios de descanso». ¿Cuál es la idea principal?',N'La idea principal resume todas las afirmaciones del texto.',N'Dormir bien beneficia la salud y debe cuidarse',N'Los horarios siempre son iguales',N'La concentración solo depende de estudiar',N'El ánimo no cambia'),
('COM_IDEA',2,N'«Los árboles dan sombra, producen oxígeno y protegen el suelo. Cuidarlos beneficia a toda la comunidad». ¿Cuál es la idea principal?',N'Busca la afirmación que integra los beneficios mencionados.',N'Los árboles son valiosos y debemos cuidarlos',N'La sombra aparece al mediodía',N'El suelo siempre está húmedo',N'Las comunidades tienen calles'),
('COM_INF',1,N'Mateo guardó su cuaderno, cerró la mochila y se despidió de sus compañeros. ¿Qué se puede inferir?',N'Las acciones permiten deducir una situación no expresada literalmente.',N'La clase terminó',N'La clase recién comienza',N'Mateo perdió la mochila',N'Nadie fue a estudiar'),
('COM_INF',2,N'Las calles estaban mojadas y varias personas sacudían sus paraguas al entrar. ¿Qué se puede inferir?',N'Relaciona las pistas para formular una conclusión razonable.',N'Había llovido',N'Hacía mucho calor',N'No había nubes',N'Las calles fueron pintadas'),
('COM_VOC',1,N'En «La estudiante explicó claramente su procedimiento», ¿qué significa claramente?',N'El contexto indica que la explicación se entendió con facilidad.',N'De manera comprensible',N'De forma secreta',N'Con mucho ruido',N'Sin terminar'),
('COM_VOC',2,N'En «El equipo resolvió el problema de manera eficiente», ¿qué significa eficiente?',N'El contexto alude a lograr un resultado usando bien el tiempo y los recursos.',N'Que logra buenos resultados con los recursos disponibles',N'Que trabaja sin objetivo',N'Que evita completar la tarea',N'Que repite todo innecesariamente'),
('COM_GRA',1,N'Elige la oración escrita correctamente.',N'Revisa mayúsculas, concordancia y punto final.',N'María y José prepararon la exposición.',N'maría y José preparó la exposición',N'María y José prepararon la exposición',N'María y josé prepara la exposición.'),
('COM_GRA',2,N'¿Qué oración utiliza correctamente la tilde?',N'Identifica la palabra que requiere tilde según las reglas de acentuación.',N'El público aplaudió la presentación.',N'El publico aplaudio la presentación.',N'El público aplaudio la presentacion.',N'El publico aplaudió la presentacion.'),
('COM_COH',1,N'Ordena la secuencia: 1) Después revisó el borrador. 2) Primero organizó sus ideas. 3) Finalmente publicó el texto.',N'Los conectores temporales muestran el orden lógico.',N'2 - 1 - 3',N'1 - 3 - 2',N'3 - 2 - 1',N'2 - 3 - 1'),
('COM_COH',2,N'Elige el conector adecuado: «El equipo investigó varias fuentes; ___, comparó la información obtenida».',N'El conector debe indicar una acción posterior.',N'luego',N'sin embargo',N'porque',N'aunque');

INSERT INTO #extra_questions(topic_id, grade, difficulty, prompt, explanation, correct_answer, wrong1, wrong2, wrong3)
SELECT t.id, g.grade, l.difficulty,
       CONCAT(N'Grado ', g.grade, N', nivel ', l.level_name, N', variación ', c.variant, N'. ', c.prompt),
       c.explanation, c.correct_answer, c.wrong1, c.wrong2, c.wrong3
FROM @communication c
JOIN topics t ON t.code = c.topic_code
CROSS JOIN (VALUES (1),(2),(3),(4),(5)) g(grade)
CROSS JOIN (VALUES ('basico',N'básico'),('intermedio',N'intermedio'),('avanzado',N'avanzado')) l(difficulty, level_name);

INSERT INTO questions(topic_id, grade, difficulty, prompt, explanation)
SELECT topic_id, grade, difficulty, prompt, explanation
FROM #extra_questions;

INSERT INTO alternatives(question_id, label, text, correct, display_order)
SELECT q.id, a.label, a.answer, a.is_correct, a.display_order
FROM #extra_questions e
JOIN questions q ON q.topic_id=e.topic_id AND q.grade=e.grade AND q.difficulty=e.difficulty AND q.prompt=e.prompt
CROSS APPLY (VALUES
    ('A',e.correct_answer,CAST(1 AS BIT),1),
    ('B',e.wrong1,CAST(0 AS BIT),2),
    ('C',e.wrong2,CAST(0 AS BIT),3),
    ('D',e.wrong3,CAST(0 AS BIT),4)
) a(label,answer,is_correct,display_order);

DROP TABLE #extra_questions;
