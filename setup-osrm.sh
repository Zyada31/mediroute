#!/bin/bash

set -e

MAP_NAME="colorado-latest"
PBF_URL="https://download.geofabrik.de/north-america/us/${MAP_NAME}.osm.pbf"
DATA_DIR="${HOME}/osrm-data"

echo "📥 Downloading PBF map to $DATA_DIR..."
mkdir -p "$DATA_DIR"
cd "$DATA_DIR"

if [ ! -f "${MAP_NAME}.osm.pbf" ]; then
  curl -O "$PBF_URL"
else
  echo "✅ PBF file already exists, skipping download."
fi

echo "🔧 Running osrm-extract..."
docker run --rm --platform linux/amd64 -v "$DATA_DIR:/data" osrm/osrm-backend \
  osrm-extract -p /opt/car.lua "/data/${MAP_NAME}.osm.pbf"

echo "🚦 Running osrm-contract..."
docker run --rm --platform linux/amd64 -v "$DATA_DIR:/data" osrm/osrm-backend \
  osrm-contract "/data/${MAP_NAME}.osrm"

echo "🚀 Starting OSRM routing server at http://localhost:5000 ..."
docker run --rm -t -i -p 5000:5000 --platform linux/amd64 -v "$DATA_DIR:/data" osrm/osrm-backend \
  osrm-routed "/data/${MAP_NAME}.osrm"