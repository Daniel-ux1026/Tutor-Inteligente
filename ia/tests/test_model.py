import unittest

from train_model import generate_dataset
from app.main import PredictionInput, explanation


class TutorModelTest(unittest.TestCase):
    def test_dataset_is_reproducible_and_has_620_rows(self):
        first = generate_dataset()
        second = generate_dataset()
        self.assertEqual(len(first), 620)
        self.assertTrue(first.equals(second))

    def test_explanation_uses_real_input_values(self):
        values = PredictionInput(
            nivel_actual="intermedio", aciertos_ultimos5=4,
            errores_consecutivos=0, tiempo_promedio_seg=54,
            intentos_tema=8, racha_aciertos=4,
        )
        text = explanation(values, "intermedio")
        self.assertIn("4 aciertos", text)
        self.assertIn("0 errores", text)


if __name__ == "__main__":
    unittest.main()
