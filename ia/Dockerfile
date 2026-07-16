FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1

WORKDIR /app
COPY ia/requirements-runtime.txt requirements.txt
RUN pip install --upgrade pip && pip install --requirement requirements.txt

COPY ia/app app
COPY ia/models models
COPY ia/reports reports

RUN addgroup --system tutor && adduser --system --ingroup tutor tutor \
    && chown -R tutor:tutor /app

USER tutor
EXPOSE 8001

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8001}"]
