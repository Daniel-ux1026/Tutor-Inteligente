from __future__ import annotations

import json
from pathlib import Path
from typing import Literal

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "models" / "decision_tree_tutor.joblib"
METRICS_PATH = ROOT / "reports" / "metrics.json"
Level = Literal["basico", "intermedio", "avanzado"]


class PredictionInput(BaseModel):
    nivel_actual: Level
    aciertos_ultimos5: int = Field(ge=0, le=5)
    errores_consecutivos: int = Field(ge=0, le=20)
    tiempo_promedio_seg: float = Field(ge=0, le=3600)
    intentos_tema: int = Field(ge=0)
    racha_aciertos: int = Field(ge=0)


app = FastAPI(title="Tutor Inteligente - Servicio IA", version="1.0.0")
artifact = None


def load_artifact():
    global artifact
    if artifact is None:
        if not MODEL_PATH.exists():
            raise HTTPException(status_code=503, detail="Modelo no entrenado. Ejecuta python train_model.py")
        artifact = joblib.load(MODEL_PATH)
    return artifact


def explanation(data: PredictionInput, level: str) -> str:
    current_index = ["basico", "intermedio", "avanzado"].index(data.nivel_actual)
    target_index = ["basico", "intermedio", "avanzado"].index(level)
    if target_index > current_index:
        return f"Sube al nivel {level} porque obtuvo {data.aciertos_ultimos5} aciertos en sus últimos cinco ejercicios, tiene una racha de {data.racha_aciertos} y su tiempo promedio es {data.tiempo_promedio_seg:.0f} segundos."
    if target_index < current_index:
        return f"Baja al nivel {level} para reforzar porque registra {data.errores_consecutivos} errores consecutivos y {data.aciertos_ultimos5} aciertos en sus últimos cinco ejercicios."
    return f"Se mantiene en el nivel {level} porque obtuvo {data.aciertos_ultimos5} aciertos en sus últimos cinco ejercicios y registra {data.errores_consecutivos} errores consecutivos."


@app.get("/health")
def health():
    return {"status": "UP", "model_ready": MODEL_PATH.exists()}


@app.get("/metrics")
def metrics():
    if not METRICS_PATH.exists():
        raise HTTPException(status_code=503, detail="Métricas no disponibles. Entrena el modelo.")
    return json.loads(METRICS_PATH.read_text(encoding="utf-8"))


@app.post("/predict")
def predict(data: PredictionInput):
    loaded = load_artifact()
    frame = pd.DataFrame([data.model_dump()])
    pipeline = loaded["pipeline"]
    level = str(pipeline.predict(frame)[0])
    probabilities = {
        str(label): round(float(value), 6)
        for label, value in zip(pipeline.classes_, pipeline.predict_proba(frame)[0])
    }
    return {
        "nivel_recomendado": level,
        "explicacion": explanation(data, level),
        "version_modelo": loaded["metrics"]["model_version"],
        "probabilidades": probabilities,
    }
