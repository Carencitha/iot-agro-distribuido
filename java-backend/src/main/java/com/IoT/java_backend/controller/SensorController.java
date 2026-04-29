package com.IoT.java_backend.controller;

import com.IoT.java_backend.model.SensorReading;
import com.IoT.java_backend.repository.SensorReadingRepository;
import com.IoT.java_backend.service.SensorProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class SensorController {

    private final SensorProcessingService processingService;
    private final SensorReadingRepository repository;

    @Value("${app.node-id}")
    private String currentNodeId;

    public SensorController(
            SensorProcessingService processingService,
            SensorReadingRepository repository
    ) {
        this.processingService = processingService;
        this.repository = repository;
    }

    @PostMapping("/readings")
    public Map<String, Object> receiveReading(@RequestBody SensorReading reading) {
        validateReading(reading);

        processingService.processAsync(reading);

        return Map.of(
                "message", "Lectura recibida y enviada a procesamiento paralelo",
                "processedBy", currentNodeId,
                "receivedAt", Instant.now().toString()
        );
    }

    @GetMapping("/readings")
    public List<Map<String, Object>> getLatestReadings() {
        return repository.findLatest();
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return Map.of(
                "nodeId", currentNodeId,
                "totalReadings", repository.countAll(),
                "readingsByNode", repository.countByNode(),
                "status", "ONLINE"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "iot-java-backend",
                "nodeId", currentNodeId,
                "status", "OK"
        );
    }

    private void validateReading(SensorReading reading) {
        if (reading.getNodeId() == null || reading.getNodeId().isBlank()) {
            throw new IllegalArgumentException("nodeId es obligatorio");
        }

        if (reading.getSensorId() == null || reading.getSensorId().isBlank()) {
            throw new IllegalArgumentException("sensorId es obligatorio");
        }

        if (reading.getTemperature() == null) {
            throw new IllegalArgumentException("temperature es obligatorio");
        }

        if (reading.getHumidity() == null) {
            throw new IllegalArgumentException("humidity es obligatorio");
        }

        if (reading.getTimestamp() == null || reading.getTimestamp().isBlank()) {
            throw new IllegalArgumentException("timestamp es obligatorio");
        }
    }
}