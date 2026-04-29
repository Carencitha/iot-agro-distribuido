CREATE TABLE IF NOT EXISTS sensor_readings (
    id SERIAL PRIMARY KEY,
    node_id VARCHAR(50) NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    temperature DECIMAL(5,2) NOT NULL,
    humidity DECIMAL(5,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_by VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_node_id 
ON sensor_readings(node_id);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_sensor_id 
ON sensor_readings(sensor_id);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_timestamp 
ON sensor_readings(timestamp);