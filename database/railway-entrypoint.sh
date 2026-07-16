#!/usr/bin/env bash
set -Eeuo pipefail

DB_NAME="${DB_NAME:-TutorInteligente}"
if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB_NAME solo puede contener letras, números y guion bajo." >&2
  exit 1
fi

install -d -o mssql -g root -m 0770 /var/opt/mssql
chown -R mssql:root /var/opt/mssql
chmod -R g=u /var/opt/mssql
export HOME=/var/opt/mssql

runuser -u mssql -- /opt/mssql/bin/sqlservr &
SQL_PID=$!

stop_sql() {
  kill -TERM "${SQL_PID}" 2>/dev/null || true
  wait "${SQL_PID}" || true
}
trap stop_sql TERM INT

READY=0
for _ in $(seq 1 60); do
  if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "${MSSQL_SA_PASSWORD}" -C -Q "SELECT 1" >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 2
done

if [[ "${READY}" != "1" ]]; then
  echo "SQL Server no quedó disponible dentro del tiempo esperado." >&2
  stop_sql
  exit 1
fi

/opt/mssql-tools18/bin/sqlcmd \
  -S localhost \
  -U sa \
  -P "${MSSQL_SA_PASSWORD}" \
  -C \
  -Q "IF DB_ID(N'${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}]"

wait "${SQL_PID}"
