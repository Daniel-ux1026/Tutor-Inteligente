"""Entrena y exporta el modelo explicable del Tutor Inteligente.

Los 620 registros son sintéticos y reproducibles. Las métricas se calculan
siempre sobre el conjunto de prueba; nunca se escriben valores prefijados.
"""
from __future__ import annotations

import json
from pathlib import Path

import joblib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    ConfusionMatrixDisplay,
    precision_recall_fscore_support,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder
from sklearn.tree import DecisionTreeClassifier

ROOT = Path(__file__).resolve().parent
DATA_PATH = ROOT / "data" / "dataset_tutor_620.csv"
MODEL_PATH = ROOT / "models" / "decision_tree_tutor.joblib"
METRICS_PATH = ROOT / "reports" / "metrics.json"
CLASSES = ["basico", "intermedio", "avanzado"]
FEATURES = [
    "nivel_actual",
    "aciertos_ultimos5",
    "errores_consecutivos",
    "tiempo_promedio_seg",
    "intentos_tema",
    "racha_aciertos",
]


def generate_dataset(rows: int = 620, random_state: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(random_state)
    current = rng.choice(CLASSES, rows, p=[0.37, 0.40, 0.23])
    current_num = pd.Series(current).map({"basico": 0, "intermedio": 1, "avanzado": 2}).to_numpy()
    ability = np.clip(rng.normal(current_num / 2, 0.25, rows), 0, 1)
    hits = np.clip(np.rint(rng.binomial(5, np.clip(0.22 + ability * 0.64, 0.08, 0.94))), 0, 5).astype(int)
    errors = np.clip(rng.poisson(np.clip(2.5 - ability * 2.1, 0.25, 3.2)), 0, 5).astype(int)
    time = np.clip(rng.normal(132 - ability * 70 + errors * 7, 20, rows), 18, 240).round(1)
    attempts = rng.integers(1, 31, rows)
    streak = np.clip(hits - rng.integers(0, 3, rows), 0, 8).astype(int)

    target_num = current_num.copy()
    down = (errors >= 2) | (hits <= 1) | (time > 150)
    up = (hits >= 4) & (errors == 0) & (streak >= 3) & (time < 105) & (attempts >= 3)
    target_num[down] -= 1
    target_num[up] += 1
    target_num = np.clip(target_num, 0, 2)

    # Ruido pedagógico controlado: simula decisiones docentes no explicadas por
    # las seis variables y evita una métrica artificialmente perfecta.
    noisy = rng.random(rows) < 0.10
    target_num[noisy] = np.clip(target_num[noisy] + rng.choice([-1, 1], noisy.sum()), 0, 2)
    target = np.array(CLASSES)[target_num]
    return pd.DataFrame({
        "nivel_actual": current,
        "aciertos_ultimos5": hits,
        "errores_consecutivos": errors,
        "tiempo_promedio_seg": time,
        "intentos_tema": attempts,
        "racha_aciertos": streak,
        "nivel_recomendado": target,
    })


def train_and_export(random_state: int = 42) -> dict:
    for directory in (DATA_PATH.parent, MODEL_PATH.parent, METRICS_PATH.parent):
        directory.mkdir(parents=True, exist_ok=True)
    frame = generate_dataset(random_state=random_state)
    frame.to_csv(DATA_PATH, index=False, encoding="utf-8")
    x_train, x_test, y_train, y_test = train_test_split(
        frame[FEATURES], frame["nivel_recomendado"], test_size=0.20,
        random_state=random_state, stratify=frame["nivel_recomendado"],
    )
    preprocessor = ColumnTransformer(
        [("level", OneHotEncoder(handle_unknown="ignore"), ["nivel_actual"])],
        remainder="passthrough",
        verbose_feature_names_out=False,
    )
    model = DecisionTreeClassifier(
        max_depth=6, min_samples_leaf=8, class_weight="balanced", random_state=random_state,
    )
    pipeline = Pipeline([("preprocessor", preprocessor), ("classifier", model)])
    pipeline.fit(x_train, y_train)
    prediction = pipeline.predict(x_test)
    accuracy = accuracy_score(y_test, prediction)
    precision, recall, f1, _ = precision_recall_fscore_support(
        y_test, prediction, average="weighted", zero_division=0,
    )
    matrix = confusion_matrix(y_test, prediction, labels=CLASSES)
    report = classification_report(y_test, prediction, labels=CLASSES, output_dict=True, zero_division=0)

    feature_names = pipeline.named_steps["preprocessor"].get_feature_names_out()
    importances = dict(sorted(zip(feature_names, model.feature_importances_), key=lambda item: item[1], reverse=True))
    metrics = {
        "model_version": "decision-tree-v1",
        "generated_at": pd.Timestamp.now(tz="UTC").isoformat(),
        "dataset_rows": len(frame),
        "train_rows": len(x_train),
        "test_rows": len(x_test),
        "random_state": random_state,
        "accuracy": round(float(accuracy), 4),
        "precision_weighted": round(float(precision), 4),
        "recall_weighted": round(float(recall), 4),
        "f1_weighted": round(float(f1), 4),
        "confusion_matrix": matrix.tolist(),
        "classes": CLASSES,
        "classification_report": report,
        "feature_importance": {key: round(float(value), 6) for key, value in importances.items()},
        "limitation": "Dataset sintético; las métricas no demuestran eficacia con estudiantes reales.",
    }
    joblib.dump({"pipeline": pipeline, "metrics": metrics, "features": FEATURES}, MODEL_PATH)
    METRICS_PATH.write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")

    ConfusionMatrixDisplay(matrix, display_labels=CLASSES).plot(cmap="Blues", colorbar=False)
    plt.title("Matriz de confusión - conjunto de prueba")
    plt.tight_layout(); plt.savefig(METRICS_PATH.parent / "confusion_matrix.png", dpi=160); plt.close()

    plt.figure(figsize=(9, 5))
    labels, values = list(importances.keys()), list(importances.values())
    plt.barh(labels[::-1], values[::-1], color="#0f8a83")
    plt.xlabel("Importancia"); plt.title("Importancia de variables")
    plt.tight_layout(); plt.savefig(METRICS_PATH.parent / "feature_importance.png", dpi=160); plt.close()
    return metrics


if __name__ == "__main__":
    print(json.dumps(train_and_export(), ensure_ascii=False, indent=2))
