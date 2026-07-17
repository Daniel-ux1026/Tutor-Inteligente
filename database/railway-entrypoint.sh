#!/usr/bin/env bash
set -Eeuo pipefail

DB_NAME="${DB_NAME:-TutorInteligente}"
if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB_NAME solo puede contener letras, números y guion bajo." >&2
  exit 1
fi

DATA_DIR="/var/opt/mssql/data"
PERSISTENT_ROOT="${RAILWAY_VOLUME_MOUNT_PATH:-/var/opt/mssql/backup}"
BACKUP_DIR="${PERSISTENT_ROOT}/tutor-backups"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}.bak"
BACKUP_INTERVAL_SECONDS="${TUTOR_SQL_BACKUP_INTERVAL_SECONDS:-60}"

if [[ ! "${BACKUP_INTERVAL_SECONDS}" =~ ^[0-9]+$ ]] \
  || (( BACKUP_INTERVAL_SECONDS < 60 || BACKUP_INTERVAL_SECONDS > 3600 )); then
  echo "TUTOR_SQL_BACKUP_INTERVAL_SECONDS debe estar entre 60 y 3600." >&2
  exit 1
fi

install -d -o mssql -g root -m 0770 \
  /var/opt/mssql "${DATA_DIR}" "${PERSISTENT_ROOT}" "${BACKUP_DIR}"

chown -R mssql:root "${DATA_DIR}"
chmod -R u+rwX,g+rwX,o-rwx "${DATA_DIR}"
chown -R mssql:root "${BACKUP_DIR}"
chmod -R u+rwX,g+rwX,o-rwx "${BACKUP_DIR}"

# Conserva una posible base de usuario creada durante los intentos iniciales.
# Se copia al disco local antes de adjuntarla; nunca se abre directamente
# desde el volumen que produjo E/S desalineada con SQL Server.
if [[ ! -s "${BACKUP_FILE}" \
  && -f "${PERSISTENT_ROOT}/${DB_NAME}.mdf" \
  && -f "${PERSISTENT_ROOT}/${DB_NAME}_log.ldf" ]]; then
  cp -n -- "${PERSISTENT_ROOT}/${DB_NAME}.mdf" "${DATA_DIR}/${DB_NAME}.mdf"
  cp -n -- "${PERSISTENT_ROOT}/${DB_NAME}_log.ldf" "${DATA_DIR}/${DB_NAME}_log.ldf"
  chown mssql:root "${DATA_DIR}/${DB_NAME}.mdf" "${DATA_DIR}/${DB_NAME}_log.ldf"
  chmod 0660 "${DATA_DIR}/${DB_NAME}.mdf" "${DATA_DIR}/${DB_NAME}_log.ldf"
fi

umask 0007
export HOME=/var/opt/mssql
export MSSQL_MEMORY_LIMIT_MB="${MSSQL_MEMORY_LIMIT_MB:-4096}"

SQL_CPU_COUNT="${TUTOR_SQL_CPU_COUNT:-4}"
if [[ ! "${SQL_CPU_COUNT}" =~ ^[1-8]$ ]]; then
  echo "TUTOR_SQL_CPU_COUNT debe ser un entero entre 1 y 8." >&2
  exit 1
fi

SQL_COMMAND=(/opt/mssql/bin/sqlservr)
if command -v taskset >/dev/null 2>&1; then
  ALLOWED_CPU_LIST="$(awk '/^Cpus_allowed_list:/ { print $2 }' /proc/self/status)"
  FIRST_CPU_SEGMENT="${ALLOWED_CPU_LIST%%,*}"
  if [[ "${FIRST_CPU_SEGMENT}" == *-* ]]; then
    FIRST_CPU="${FIRST_CPU_SEGMENT%-*}"
    LAST_ALLOWED_CPU="${FIRST_CPU_SEGMENT#*-}"
  else
    FIRST_CPU="${FIRST_CPU_SEGMENT}"
    LAST_ALLOWED_CPU="${FIRST_CPU_SEGMENT}"
  fi

  LAST_CPU=$((FIRST_CPU + SQL_CPU_COUNT - 1))
  if (( LAST_CPU > LAST_ALLOWED_CPU )); then
    LAST_CPU="${LAST_ALLOWED_CPU}"
  fi

  if [[ -n "${FIRST_CPU}" && "${LAST_CPU}" -ge "${FIRST_CPU}" ]]; then
    SQL_COMMAND=(taskset -c "${FIRST_CPU}-${LAST_CPU}" /opt/mssql/bin/sqlservr)
    echo "SQL Server usará afinidad de CPU ${FIRST_CPU}-${LAST_CPU} y hasta ${MSSQL_MEMORY_LIMIT_MB} MB de memoria."
  fi
fi

SQL_PID=""
BACKUP_PID=""
DATABASE_READY=0
STARTUP_LOG="/tmp/tutor-sqlserver-startup.log"

start_sql() {
  : > "${STARTUP_LOG}"
  runuser -u mssql -- "${SQL_COMMAND[@]}" \
    > >(tee -a "${STARTUP_LOG}") 2>&1 &
  SQL_PID=$!
}

stop_sql() {
  if [[ -n "${BACKUP_PID}" ]]; then
    kill -TERM "${BACKUP_PID}" 2>/dev/null || true
    wait "${BACKUP_PID}" 2>/dev/null || true
  fi

  if [[ "${DATABASE_READY}" == "1" ]]; then
    backup_database || echo "No se pudo completar el respaldo final de ${DB_NAME}." >&2
  fi

  if [[ -n "${SQL_PID}" ]]; then
    kill -TERM "${SQL_PID}" 2>/dev/null || true
    wait "${SQL_PID}" 2>/dev/null || true
  fi
}
trap stop_sql TERM INT

wait_until_ready() {
  for _ in $(seq 1 75); do
    if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "${MSSQL_SA_PASSWORD}" -C -Q "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi

    if ! kill -0 "${SQL_PID}" 2>/dev/null; then
      wait "${SQL_PID}" 2>/dev/null || true
      sleep 1
      return 1
    fi

    sleep 2
  done

  return 1
}

archive_corrupted_system_databases() {
  local recovery_root="${DATA_DIR}/.tutor-systemdb-recovery"
  local recovery_dir="${recovery_root}/$(date -u +%Y%m%dT%H%M%SZ)"
  local moved=0

  install -d -o mssql -g root -m 0770 "${recovery_dir}"

  # Solo se apartan las bases internas. Los MDF/LDF de TutorInteligente y de
  # cualquier otra base de usuario permanecen en su ubicación original.
  for filename in \
    master.mdf mastlog.ldf \
    model.mdf modellog.ldf model_msdbdata.mdf model_msdblog.ldf \
    MSDBData.mdf MSDBLog.ldf \
    tempdb.mdf templog.ldf tempdb_mssql_*.ndf; do
    for source in "${DATA_DIR}"/${filename}; do
      if [[ -e "${source}" ]]; then
        mv -- "${source}" "${recovery_dir}/"
        moved=1
      fi
    done
  done

  if [[ "${moved}" != "1" ]]; then
    rmdir "${recovery_dir}" 2>/dev/null || true
    return 1
  fi

  chown -R mssql:root "${recovery_root}"
  chmod -R u+rwX,g+rwX,o-rwx "${recovery_root}"
  touch "${recovery_root}/recovery-attempted-v2"
  chown mssql:root "${recovery_root}/recovery-attempted-v2"
}

backup_database() {
  local temporary_backup="${BACKUP_FILE}.tmp"

  rm -f -- "${temporary_backup}"
  /opt/mssql-tools18/bin/sqlcmd \
    -S localhost \
    -U sa \
    -P "${MSSQL_SA_PASSWORD}" \
    -C \
    -b \
    -Q "BACKUP DATABASE [${DB_NAME}] TO DISK = N'${temporary_backup}' WITH INIT, CHECKSUM, COMPRESSION"
  mv -f -- "${temporary_backup}" "${BACKUP_FILE}"
  chown mssql:root "${BACKUP_FILE}"
  chmod 0660 "${BACKUP_FILE}"
}

start_sql

if ! wait_until_ready; then
  RECOVERY_MARKER="${DATA_DIR}/.tutor-systemdb-recovery/recovery-attempted-v2"
  if [[ ! -e "${RECOVERY_MARKER}" ]] \
    && grep -Fq "Starting up database 'master'" "${STARTUP_LOG}" \
    && grep -Fq "Message: Stack Overflow" "${STARTUP_LOG}"; then
    echo "Se detectó daño en las bases internas; se archivarán antes de regenerarlas..."
    stop_sql
    archive_corrupted_system_databases
    start_sql

    if ! wait_until_ready; then
      echo "SQL Server no pudo iniciar después de regenerar sus bases internas." >&2
      stop_sql
      exit 1
    fi
  else
    echo "SQL Server no quedó disponible dentro del tiempo esperado." >&2
    stop_sql
    exit 1
  fi
fi

if [[ -s "${BACKUP_FILE}" ]]; then
  echo "Restaurando ${DB_NAME} desde el volumen persistente..."
  /opt/mssql-tools18/bin/sqlcmd \
    -S localhost \
    -U sa \
    -P "${MSSQL_SA_PASSWORD}" \
    -C \
    -b \
    -Q "IF DB_ID(N'${DB_NAME}') IS NULL RESTORE DATABASE [${DB_NAME}] FROM DISK = N'${BACKUP_FILE}' WITH MOVE N'${DB_NAME}' TO N'${DATA_DIR}/${DB_NAME}.mdf', MOVE N'${DB_NAME}_log' TO N'${DATA_DIR}/${DB_NAME}_log.ldf', RECOVERY"
fi

if [[ -f "${DATA_DIR}/${DB_NAME}.mdf" && -f "${DATA_DIR}/${DB_NAME}_log.ldf" ]]; then
  /opt/mssql-tools18/bin/sqlcmd \
    -S localhost \
    -U sa \
    -P "${MSSQL_SA_PASSWORD}" \
    -C \
    -Q "IF DB_ID(N'${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}] ON (FILENAME = N'${DATA_DIR}/${DB_NAME}.mdf'), (FILENAME = N'${DATA_DIR}/${DB_NAME}_log.ldf') FOR ATTACH"
fi

/opt/mssql-tools18/bin/sqlcmd \
  -S localhost \
  -U sa \
  -P "${MSSQL_SA_PASSWORD}" \
  -C \
  -Q "IF DB_ID(N'${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}]"

DATABASE_READY=1
backup_database

(
  while sleep "${BACKUP_INTERVAL_SECONDS}"; do
    backup_database || echo "No se pudo actualizar el respaldo periódico de ${DB_NAME}." >&2
  done
) &
BACKUP_PID=$!

echo "TUTOR_SQLSERVER_READY"

wait "${SQL_PID}"
