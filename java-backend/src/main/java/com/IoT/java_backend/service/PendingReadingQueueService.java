package com.IoT.java_backend.service;

import com.IoT.java_backend.model.SensorReading;
import com.IoT.java_backend.repository.SensorReadingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PendingReadingQueueService {

    private final SensorReadingRepository repository;
    private final ObjectMapper objectMapper;

    private final Path queueFile = Path.of("pending-readings.jsonl");

    public PendingReadingQueueService(SensorReadingRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    /*
        Guarda una lectura pendiente en un archivo local cuando PostgreSQL
        no está disponible.
    */
    public synchronized void enqueue(SensorReading reading, String status) {
        try {
            PendingReading pendingReading = new PendingReading();
            pendingReading.setReading(reading);
            pendingReading.setStatus(status);
            pendingReading.setQueuedAt(Instant.now().toString());

            String jsonLine = objectMapper.writeValueAsString(pendingReading);

            Files.writeString(
                    queueFile,
                    jsonLine + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println("Lectura guardada en cola local: " + queueFile.toAbsolutePath());

        } catch (Exception error) {
            System.out.println("Error guardando lectura en cola local: " + error.getMessage());
        }
    }

    /*
        Cada 10 segundos intenta sincronizar las lecturas pendientes.
        Si PostgreSQL ya volvió, guarda las lecturas pendientes y limpia la cola.
        Si PostgreSQL sigue caído, conserva las lecturas en el archivo.
    */
    @Scheduled(fixedDelay = 10000)
    public synchronized void syncPendingReadings() {
        try {
            if (!Files.exists(queueFile)) {
                return;
            }

            List<String> lines = Files.readAllLines(queueFile, StandardCharsets.UTF_8);

            if (lines.isEmpty()) {
                return;
            }

            List<String> remainingLines = new ArrayList<>();
            int syncedCount = 0;

            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                try {
                    PendingReading pendingReading = objectMapper.readValue(line, PendingReading.class);

                    repository.save(
                            pendingReading.getReading(),
                            pendingReading.getStatus()
                    );

                    syncedCount++;

                } catch (Exception error) {
                    remainingLines.add(line);
                }
            }

            updateQueueFile(remainingLines);

            if (syncedCount > 0) {
                System.out.println("Lecturas sincronizadas desde cola local: " + syncedCount);
            }

        } catch (Exception error) {
            System.out.println("Error sincronizando cola local: " + error.getMessage());
        }
    }

    private void updateQueueFile(List<String> remainingLines) {
        try {
            if (remainingLines.isEmpty()) {
                Files.deleteIfExists(queueFile);
                return;
            }

            Files.write(
                    queueFile,
                    remainingLines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (Exception error) {
            System.out.println("Error actualizando archivo de cola local: " + error.getMessage());
        }
    }

    public static class PendingReading {

        private SensorReading reading;
        private String status;
        private String queuedAt;

        public PendingReading() {
        }

        public SensorReading getReading() {
            return reading;
        }

        public void setReading(SensorReading reading) {
            this.reading = reading;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getQueuedAt() {
            return queuedAt;
        }

        public void setQueuedAt(String queuedAt) {
            this.queuedAt = queuedAt;
        }
    }
}