#!/bin/bash
# startup.sh - MediRoute Application Startup Script

echo "🏥 Starting MediRoute Medical Transport System..."

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432; then
    echo "❌ PostgreSQL is not running. Please start PostgreSQL first."
    echo "   brew services start postgresql  # On macOS"
    echo "   sudo systemctl start postgresql # On Linux"
    exit 1
fi

# Check if database exists
if ! psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw mediroute; then
    echo "📦 Creating mediroute database..."
    createdb -h localhost -U postgres mediroute
fi

# Set environment variables
export GOOGLE_API_KEY="${GOOGLE_API_KEY:-your_google_api_key_here}"
export OSRM_BASE_URL="${OSRM_BASE_URL:-http://localhost:5000}"

# Check if OSRM is running (optional)
if ! curl -s "$OSRM_BASE_URL/route/v1/driving/13.388860,52.517037;13.397634,52.529407" > /dev/null; then
    echo "⚠️  OSRM service not available at $OSRM_BASE_URL"
    echo "   The application will use fallback distance calculations."
fi


echo "🚀 Starting Spring Boot application..."
# Run the application
./mvnw spring-boot:run