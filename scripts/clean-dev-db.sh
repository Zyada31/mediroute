#!/usr/bin/env bash
set -euo pipefail

# Clean the dev Postgres database by dropping and recreating it (default),
# or by dropping/recreating the public schema only.
#
# Env vars (override as needed):
#   USE_DOCKER=0|1                    # Use Docker container
#   DOCKER_POSTGRES_CONTAINER=name    # Default: mediroute_postgres
#   DB_HOST=localhost
#   DB_PORT=5432
#   DB_NAME=mediroute
#   DB_USER=postgres
#   DB_PASSWORD=postgres
#   DROP_SCHEMA_ONLY=0|1              # If 1, only reset schema, not the whole DB

USE_DOCKER=${USE_DOCKER:-0}
DOCKER_POSTGRES_CONTAINER=${DOCKER_POSTGRES_CONTAINER:-mediroute_postgres}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-mediroute}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}
DROP_SCHEMA_ONLY=${DROP_SCHEMA_ONLY:-0}

echo "🧹 Cleaning dev database '${DB_NAME}' (DROP_SCHEMA_ONLY=${DROP_SCHEMA_ONLY}, USE_DOCKER=${USE_DOCKER})"

psql_local() {
  PGPASSWORD="${DB_PASSWORD}" psql \
    -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" "$@"
}

psql_docker() {
  docker exec -e PGPASSWORD="${DB_PASSWORD}" -u postgres "${DOCKER_POSTGRES_CONTAINER}" \
    psql -h localhost -p 5432 -U "${DB_USER}" "$@"
}

drop_and_recreate_db_local() {
  echo "🔌 Terminating connections to ${DB_NAME} (local)"
  psql_local -d postgres -v ON_ERROR_STOP=1 -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" || true
  echo "🗑️  Dropping database ${DB_NAME} (local)"
  psql_local -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"${DB_NAME}\" WITH (FORCE);"
  echo "📦 Creating database ${DB_NAME} (local)"
  psql_local -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USER}\";"
}

drop_and_recreate_db_docker() {
  echo "🔌 Terminating connections to ${DB_NAME} (docker:${DOCKER_POSTGRES_CONTAINER})"
  psql_docker -d postgres -v ON_ERROR_STOP=1 -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" || true
  echo "🗑️  Dropping database ${DB_NAME} (docker)"
  psql_docker -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"${DB_NAME}\" WITH (FORCE);"
  echo "📦 Creating database ${DB_NAME} (docker)"
  psql_docker -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USER}\";"
}

reset_schema_local() {
  echo "🧯 Dropping and recreating schema public (local) in ${DB_NAME}"
  psql_local -d "${DB_NAME}" -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO public;"
}

reset_schema_docker() {
  echo "🧯 Dropping and recreating schema public (docker) in ${DB_NAME}"
  psql_docker -d "${DB_NAME}" -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO public;"
}

if [[ "${USE_DOCKER}" == "1" ]]; then
  if ! docker ps --format '{{.Names}}' | grep -qx "${DOCKER_POSTGRES_CONTAINER}"; then
    echo "❌ Docker container ${DOCKER_POSTGRES_CONTAINER} not running. Start it first: docker compose up -d postgres" >&2
    exit 1
  fi
  if [[ "${DROP_SCHEMA_ONLY}" == "1" ]]; then
    reset_schema_docker
  else
    drop_and_recreate_db_docker
  fi
else
  # local
  if [[ "${DROP_SCHEMA_ONLY}" == "1" ]]; then
    reset_schema_local
  else
    drop_and_recreate_db_local
  fi
fi

echo "✅ Dev database cleaned: ${DB_NAME}"


