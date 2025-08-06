
```markdown
# Preparing OSRM Map Data (Apple Silicon / ARM64)

Follow these steps to set up OSRM map data on your machine.

---

## 🗺 Step 1: Download a PBF Map File

Choose a region (e.g., Colorado):

```bash
mkdir -p ~/osrm-data && cd ~/osrm-data
curl -O https://download.geofabrik.de/north-america/us/colorado-latest.osm.pbf
```

## Step 2: Extract and Contract the Data

Use Docker with Apple Silicon (ARM64):

```bash
docker run --platform linux/amd64 -v "$PWD:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/colorado-latest.osm.pbf
docker run --platform linux/amd64 -v "$PWD:/data" osrm/osrm-backend osrm-contract /data/colorado-latest.osrm
```

## Step 3: Start the OSRM Server

Run the OSRM server with your processed data:

```bash
docker run --platform linux/amd64 -t -i -p 5000:5000 -v "$PWD:/data" osrm/osrm-backend osrm-routed /data/colorado-latest.osrm
```

## Step 4: Test the Server

Open your browser and go to:

```
http://localhost:5000/route/v1/driving/39.7392,-104.9903;39.7392,-105.9903?overview=false
```

You should see a JSON response with route information.

---
## Additional Notes
```# Default (Colorado)
./setup-osrm.sh

# New region (e.g., texas, california, ohio)
./setup-osrm.sh texas
# New region (e.g., texas, california, ohio)```

# For more information, visit the [OSRM documentation](http://project-osrm.org/docs/v5.24.0/api/#route-service).
```
#app running
╰─ mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.library.path=/Users/semereghebrechristos/Downloads/jni-lib/ortools-darwin-aarch64"                                                        ─╯
Area
Options
🧠 Optimization
Fine-tune OR-Tools constraints (max ride per driver, time windows, cost penalties)
⚙️ Caching
Cache OSRM results using Valkey to reduce API load
🌍 Map Preview
Add Leaflet or Mapbox route preview per assignment
📊 Dispatch UI
Display assignments per driver + unassigned list
🧪 Testing
Add JUnit + integration tests for DriverAssignmentService and RideOptimizerService
🧮 Cost Estimation
Use distanceService.getDistanceInMeters() and calculate dynamic estimated cost
