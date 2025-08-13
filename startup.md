### MediRoute local startup guide

This guide covers running the full stack locally with Docker Compose, using `startup.sh`, and configuring `.env`.

### Prerequisites
- Docker Desktop (with Docker Compose)
- macOS/Linux shell (zsh/bash)
- Java 21 (only if you run locally without Docker)

### .env configuration (optional but recommended)
Create a `.env` file at the repo root to persist environment variables. Example:

```
# App profile
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

# Database (Compose defaults)
POSTGRES_DB=mediroute
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# External APIs
GOOGLE_API_KEY=
OSRM_BASE_URL=http://localhost:5000

# JWT dev keys (if not set, startup.sh will generate under keys/)
JWT_PRIVATE_PEM=
JWT_PUBLIC_PEM=

# Orchestration flags for startup.sh
USE_DOCKER=1        # Start Postgres/Redis (and optionally OSRM) via Docker Compose
OSRM_PREP=0         # Set to 1 to download + prepare OSRM data
START_OSRM=0        # Set to 1 to start OSRM service after prep

# OSRM dataset selection
MAP_NAME=colorado-latest
OSRM_DATA_DIR=./osrm-data
# Optional: override the computed Geofabrik PBF URL
# PBF_URL=https://download.geofabrik.de/north-america/us/colorado-latest.osm.pbf
```

Note: `.env` is loaded automatically by `startup.sh` if present.

### Fast path: bring up dependencies and the app
- Ensure Docker Desktop is running
- From the repo root:
```
USE_DOCKER=1 ./startup.sh
```
This starts Postgres and Redis via Docker Compose, waits until Postgres is ready, then runs the Spring app via `mvnw spring-boot:run`.

Verify:
- API health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Include OSRM for distance/routing
First run (downloads PBF and prepares data, then starts OSRM):
```
USE_DOCKER=1 OSRM_PREP=1 START_OSRM=1 MAP_NAME=colorado-latest ./startup.sh
```
Verify OSRM: `http://localhost:5000`

You can persist these flags in `.env` so the command is simply:
```
./startup.sh
```

### Manual OSRM preparation (alternative)
Use the compose "prep" profile to run steps explicitly:
```
# Download the PBF
MAP_NAME=colorado-latest OSRM_DATA_DIR=./osrm-data \
  docker compose --profile prep run --rm osrm-download

# Extract + partition/customize (or contract)
MAP_NAME=colorado-latest OSRM_DATA_DIR=./osrm-data \
  docker compose --profile prep run --rm osrm-prepare

# Start the OSRM router
docker compose up -d osrm
```

### Common environment variables
- `SPRING_PROFILES_ACTIVE`: Spring profile (default `dev`)
- `GOOGLE_API_KEY`: Google Geocoding API key (optional in dev)
- `JWT_PRIVATE_PEM` / `JWT_PUBLIC_PEM`: PEM content. If not set, `startup.sh` generates dev keys to `keys/` and exports them.
- `MAP_NAME`: OSRM dataset (e.g., `colorado-latest`, `arizona-latest`)
- `OSRM_DATA_DIR`: where OSRM data files are stored (default `./osrm-data`)
- `PBF_URL`: override Geofabrik URL if needed

### Troubleshooting
- Blank PBF URL: make sure you didn’t pass an empty `PBF_URL`. Remove any partial file and re-run download:
```
rm -f ./osrm-data/${MAP_NAME:-colorado-latest}.osm.pbf
MAP_NAME=colorado-latest OSRM_DATA_DIR=./osrm-data \
  docker compose --profile prep run --rm osrm-download
```
- OSRM not responding: ensure the dataset was prepared successfully and the router is running: `docker compose ps` and `docker compose logs osrm`.
- Port conflicts: stop any local Postgres/Redis/OSRM listening on 5432/6379/5000.
- Compose “version is obsolete” warning: safe to ignore; we’ll remove the key in a later cleanup.

### Security (dev-only)
- Dev JWT keys are generated automatically if not provided. Don’t use them in production.
- Keep `login.json`, `.env`, and key files out of git. The repo’s `.gitignore` already excludes them.

### Useful endpoints
- Health: `GET /actuator/health`
- API docs: `GET /swagger-ui.html`
- OpenAPI JSON: `GET /api-docs`

### Stop and cleanup
```
# Stop app (if running in foreground, Ctrl+C). Stop containers:
docker compose down

# Remove OSRM and DB volumes (destructive!)
docker compose down -v
```
