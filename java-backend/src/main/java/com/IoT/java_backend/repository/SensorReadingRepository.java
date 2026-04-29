package com.IoT.java_backend.repository;

import com.IoT.java_backend.model.SensorReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class SensorReadingRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.node-id}")
    private String currentNodeId;

    public SensorReadingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(SensorReading reading, String status) {
        String sql = """
                INSERT INTO sensor_readings
                (node_id, sensor_id, temperature, humidity, status, processed_by, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                reading.getNodeId(),
                reading.getSensorId(),
                reading.getTemperature(),
                reading.getHumidity(),
                status,
                currentNodeId,
                Timestamp.from(Instant.parse(reading.getTimestamp()))
        );
    }

    public List<Map<String, Object>> findLatest() {
        String sql = """
                SELECT id, node_id, sensor_id, temperature, humidity, status, processed_by, timestamp, created_at
                FROM sensor_readings
                ORDER BY created_at DESC
                LIMIT 50
                """;

        return jdbcTemplate.queryForList(sql);
    }

    public Integer countAll() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sensor_readings",
                Integer.class
        );
    }

    public List<Map<String, Object>> countByNode() {
        String sql = """
                SELECT node_id, COUNT(*) AS total
                FROM sensor_readings
                GROUP BY node_id
                ORDER BY node_id
                """;

        return jdbcTemplate.queryForList(sql);
    }
}