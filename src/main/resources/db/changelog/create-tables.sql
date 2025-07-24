-- drivers table
CREATE TABLE IF NOT EXISTS drivers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    vehicle_type VARCHAR(20) CHECK (vehicle_type IN ('sedan', 'van', 'wheelchair_van')) DEFAULT 'sedan',
    skills JSONB NOT NULL DEFAULT '{"wheelchair": false}',
    base_location VARCHAR(200) DEFAULT 'Driver Base, 123 Main St',
    active BOOLEAN DEFAULT TRUE

);

-- patients table
CREATE TABLE IF NOT EXISTS patients (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(100),
    default_pickup_location VARCHAR(200),
    default_dropoff_location VARCHAR(200),
    requires_wheelchair BOOLEAN DEFAULT FALSE,
    special_needs JSONB DEFAULT '{}'
);

-- rides table
CREATE TABLE IF NOT EXISTS rides (
    id SERIAL PRIMARY KEY,
    patient_id INT REFERENCES patients(id),
    driver_id INT REFERENCES drivers(id),
    pickup_location VARCHAR(200) NOT NULL,
    dropoff_location VARCHAR(200) NOT NULL,
    pickup_time TIMESTAMP NOT NULL,
    wait_time INT DEFAULT 0 CHECK (wait_time BETWEEN 0 AND 15),
    is_sequential BOOLEAN DEFAULT FALSE,
    distance FLOAT,
    status VARCHAR(20) CHECK (status IN ('scheduled', 'in_progress', 'completed', 'canceled')) DEFAULT 'scheduled',
    required_vehicle_type VARCHAR(20) CHECK (required_vehicle_type IN ('sedan', 'van', 'wheelchair_van')),
    required_skills JSONB DEFAULT '{}'
);

-- schedules table
CREATE TABLE IF NOT EXISTS schedules (
    id SERIAL PRIMARY KEY,
    ride_id INT REFERENCES rides(id),
    date DATE NOT NULL,
    assigned_driver_id INT REFERENCES drivers(id)
);

-- patient_history table
CREATE TABLE IF NOT EXISTS patient_history (
    id SERIAL PRIMARY KEY,
    patient_id INT REFERENCES patients(id),
    ride_id INT REFERENCES rides(id),
    date TIMESTAMP,
    distance FLOAT
);

ALTER TABLE drivers
ADD COLUMN IF NOT EXISTS shift_start TIMESTAMP,
ADD COLUMN IF NOT EXISTS shift_end TIMESTAMP;
ALTER TABLE drivers ADD COLUMN max_daily_rides INT DEFAULT 8;