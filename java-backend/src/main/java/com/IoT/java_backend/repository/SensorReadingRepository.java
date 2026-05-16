package com.IoT.java_backend.repository;

import com.IoT.java_backend.model.SensorReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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

    /*
        Vista normal del dashboard:
        Solo trae las últimas 50 lecturas para no saturar la tabla.
    */
    public List<Map<String, Object>> findLatest() {
        String sql = """
                SELECT id, node_id, sensor_id, temperature, humidity, status, processed_by, timestamp, created_at
                FROM sensor_readings
                ORDER BY created_at DESC
                LIMIT 50
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /*
        Vista filtrada del dashboard:
        Busca en el histórico completo según los filtros enviados.
        Permite consultar datos antiguos sin cargar todo siempre.
    */
    public List<Map<String, Object>> findFiltered(String nodeId, String sensor, String date) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, node_id, sensor_id, temperature, humidity, status, processed_by, timestamp, created_at
                FROM sensor_readings
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (hasText(nodeId)) {
            sql.append(" AND node_id = ? ");
            params.add(nodeId);
        }

        if (hasText(sensor)) {
            if ("manual".equalsIgnoreCase(sensor)) {
                sql.append(" AND LOWER(sensor_id) LIKE ? ");
                params.add("%manual%");
            } else {
                /*
                    Esto permite que sensor-1 encuentre:
                    machine-1-sensor-1
                    machine-2-sensor-1
                    machine-3-sensor-1
                    machine-4-sensor-1
                    manual-sensor-1
                */
                sql.append(" AND LOWER(sensor_id) LIKE ? ");
                params.add("%" + sensor.toLowerCase());
            }
        }

        if (hasText(date)) {
            /*
                date llega desde el input HTML como YYYY-MM-DD.
                Se compara contra la fecha del timestamp.
            */
            sql.append(" AND DATE(timestamp) = CAST(? AS DATE) ");
            params.add(date);
        }

        sql.append(" ORDER BY created_at DESC ");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"all".equalsIgnoreCase(value);
    }
}