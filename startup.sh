#!/usr/bin/env bash
set -euo pipefail

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
info() { printf "ℹ️  %s\n" "$*"; }
ok()   { printf "✅ %s\n" "$*"; }
warn() { printf "⚠️  %s\n" "$*"; }
err()  { printf "❌ %s\n" "$*" >&2; }

bold "🏥 Starting MediRoute…"

# 0) Load .env if present
if [[ -f .env ]]; then
  info "Loading environment from .env"
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

# 1) Required tools
command -v psql >/dev/null || { err "psql not found"; exit 1; }
command -v pg_isready >/dev/null || { err "pg_isready not found"; exit 1; }
command -v curl >/dev/null || { err "curl not found"; exit 1; }
command -v openssl >/dev/null || warn "openssl not found (dev keygen will be skipped)"

# 2) Postgres defaults
: "${PGHOST:=localhost}"
: "${PGPORT:=5432}"
: "${PGUSER:=postgres}"
: "${PGDATABASE:=mediroute}"
: "${PGPASSWORD:=}"

# 3) Optionally start local deps with Docker Compose
if [[ "${USE_DOCKER:-0}" == "1" ]]; then
  if command -v docker >/dev/null 2>&1; then
    if docker compose version >/dev/null 2>&1; then
      info "Starting local dependencies via docker compose"
      # Optional OSRM data prep
      if [[ "${OSRM_PREP:-0}" == "1" ]]; then
        : "${MAP_NAME:=colorado-latest}"
        : "${OSRM_DATA_DIR:=./osrm-data}"
        info "Preparing OSRM dataset ($MAP_NAME) into $OSRM_DATA_DIR"
        MAP_NAME="$MAP_NAME" OSRM_DATA_DIR="$OSRM_DATA_DIR" PBF_URL="${PBF_URL:-}" \
          docker compose --profile prep run --rm osrm-download
        MAP_NAME="$MAP_NAME" OSRM_DATA_DIR="$OSRM_DATA_DIR" \
          docker compose --profile prep run --rm osrm-prepare
      fi
      docker compose up -d postgres redis
      if [[ "${START_OSRM:-0}" == "1" ]]; then
        docker compose up -d osrm
        : "${OSRM_BASE_URL:=http://localhost:5000}"
        export OSRM_BASE_URL
      fi
    else
      warn "docker compose not available; skipping containerized deps"
    fi
  else
    warn "docker not found; skipping containerized deps"
  fi
fi

# 4) DB up? (wait up to ~60s)
for i in {1..30}; do
  if pg_isready -h "$PGHOST" -p "$PGPORT" >/dev/null 2>&1; then
    ok "PostgreSQL reachable"
    break
  fi
  if [[ $i -eq 30 ]]; then
    err "PostgreSQL is not running at $PGHOST:$PGPORT"
    exit 1
  fi
  sleep 2
done

# 5) Ensure DB exists
if ! psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -lqt | cut -d \| -f1 | tr -d ' ' | grep -qw "$PGDATABASE"; then
  info "Creating database $PGDATABASE…"
  createdb -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$PGDATABASE"
  ok "Database created"
else
  ok "Database exists"
fi

# 6) Optional OSRM (dev)
#if [[ "${START_OSRM_DEV:-0}" == "1" ]]; then
#  if command -v docker >/dev/null; then
#    : "${OSRM_PORT:=5000}"
#    : "${OSRM_REGION:=monaco}"
#    : "${OSRM_DATA_DIR:=$HOME/.mediroute/osrm/$OSRM_REGION}"
#    mkdir -p "$OSRM_DATA_DIR"
#    if [[ ! -f "$OSRM_DATA_DIR/$OSRM_REGION.osrm" ]]; then
#      info "Preparing OSRM data ($OSRM_REGION)…"
#      curl -L "https://download.geofabrik.de/europe/monaco-latest.osm.pbf" -o "$OSRM_DATA_DIR/$OSRM_REGION.osm.pbf"
#      docker run --rm -v "$OSRM_DATA_DIR":/data ghcr.io/project-osrm/osrm-backend:latest osrm-extract -p /opt/car.lua /data/$OSRM_REGION.osm.pbf
#      docker run --rm -v "$OSRM_DATA_DIR":/data ghcr.io/project-osrm/osrm-backend:latest osrm-partition /data/$OSRM_REGION.osrm
#      docker run --rm -v "$OSRM_DATA_DIR":/data ghcr.io/project-osrm/osrm-backend:latest osrm-customize /data/$OSRM_REGION.osrm
#      ok "OSRM data ready"
#    fi
#    if ! docker ps --format '{{.Names}}' | grep -q '^mediroute-osrm$'; then
#      info "Starting OSRM dev container on :$OSRM_PORT"
#      docker run -d --name mediroute-osrm --restart unless-stopped \
#        -p "$OSRM_PORT:5000" -v "$OSRM_DATA_DIR":/data \
#        ghcr.io/project-osrm/osrm-backend:latest \
#        osrm-routed --algorithm mld /data/$OSRM_REGION.osrm
#      ok "OSRM running at http://localhost:$OSRM_PORT"
#    else
#      ok "OSRM already running"
#    fi
#    export OSRM_BASE_URL="${OSRM_BASE_URL:-http://localhost:$OSRM_PORT}"
#  else
#    warn "Docker not found; skipping OSRM"
#  fi
#fi
#: "${OSRM_BASE_URL:=http://localhost:5000}"
#curl -s --max-time 2 "$OSRM_BASE_URL/route/v1/driving/13.38886,52.517037;13.397634,52.529407" >/dev/null || \
#  warn "OSRM not reachable at $OSRM_BASE_URL — using fallback distances"

# 7) Spring profile
: "${SPRING_PROFILES_ACTIVE:=dev}"
export SPRING_PROFILES_ACTIVE

# 8) JWT keys — ensure PEM vars are set (generate dev keys if missing)
KEYS_DIR="./keys"
PRIV_FILE="$KEYS_DIR/jwt-private.pem"
PUB_FILE="$KEYS_DIR/jwt-public.pem"

# If env doesn’t have keys, try files, else generate
if [[ -z "${JWT_PRIVATE_PEM:-}" || -z "${JWT_PUBLIC_PEM:-}" ]]; then
  if [[ -f "$PRIV_FILE" && -f "$PUB_FILE" ]]; then
    info "Loading JWT keys from $KEYS_DIR"
    export JWT_PRIVATE_PEM="$(cat "$PRIV_FILE")"
    export JWT_PUBLIC_PEM="$(cat "$PUB_FILE")"
  else
    if command -v openssl >/dev/null; then
      warn "JWT keys not found; generating dev keys in $KEYS_DIR"
      mkdir -p "$KEYS_DIR"
      openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIV_FILE"
      openssl rsa -pubout -in "$PRIV_FILE" -out "$PUB_FILE"
      export JWT_PRIVATE_PEM="$(cat "$PRIV_FILE")"
      export JWT_PUBLIC_PEM="$(cat "$PUB_FILE")"
      ok "Dev JWT keys generated"
    else
      err "No JWT keys and openssl missing. Set JWT_PRIVATE_PEM/JWT_PUBLIC_PEM or add keys in $KEYS_DIR."
      exit 1
    fi
  fi
fi

# 9) Launch Spring
info "Starting Spring Boot (profile=$SPRING_PROFILES_ACTIVE)…"
./mvnw spring-boot:run