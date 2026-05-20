package com.IoT.java_backend.service;

import com.IoT.java_backend.model.SensorReading;
import com.IoT.java_backend.repository.SensorReadingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SensorProcessingService {

    private final SensorReadingRepository repository;
    private final PendingReadingQueueService pendingQueueService;

    @Value("${app.processing-threads}")
    private int processingThreads;

    @Value("${app.node-id}")
    private String currentNodeId;

    private ExecutorService executorService;

    public SensorProcessingService(
            SensorReadingRepository repository,
            PendingReadingQueueService pendingQueueService
    ) {
        this.repository = repository;
        this.pendingQueueService = pendingQueueService;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(processingThreads);

        System.out.println("====================================");
        System.out.println("Nodo activo: " + currentNodeId);
        System.out.println("Hilos de procesamiento: " + processingThreads);
        System.out.println("Cola local de respaldo: ACTIVADA");
        System.out.println("====================================");
    }

    public void processAsync(SensorReading reading) {
        executorService.submit(() -> {
            String status = analyzeStatus(reading);

            try {
                repository.save(reading, status);

                System.out.println(
                        "Dato procesado por " + currentNodeId +
                                " | Sensor: " + reading.getSensorId() +
                                " | Temp: " + reading.getTemperature() +
                                " | Humedad: " + reading.getHumidity() +
                                " | Estado: " + status +
                                " | Guardado en PostgreSQL"
                );

            } catch (Exception error) {
                pendingQueueService.enqueue(reading, status);

                System.out.println(
                        "No se pudo guardar en PostgreSQL. Lectura enviada a cola local." +
                                " | Nodo: " + reading.getNodeId() +
                                " | Sensor: " + reading.getSensorId() +
                                " | Error: " + error.getMessage()
                );
            }
        });
    }

    private String analyzeStatus(SensorReading reading) {
        double temperature = reading.getTemperature();
        double humidity = reading.getHumidity();

        if (temperature >= 35) {
            return "TEMP_ALTA";
        }

        if (temperature <= 10) {
            return "TEMP_BAJA";
        }

        if (humidity >= 85) {
            return "HUMEDAD_ALTA";
        }

        if (humidity <= 30) {
            return "HUMEDAD_BAJA";
        }

        return "NORMAL";
    }
}