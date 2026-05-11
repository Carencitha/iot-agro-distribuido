async function loadDashboard() {
    await loadMetrics();
    await loadReadings();
}

async function loadMetrics() {
    try {
        const response = await fetch("/api/metrics");
        const data = await response.json();

        document.getElementById("nodeId").textContent = data.nodeId;
        document.getElementById("totalReadings").textContent = data.totalReadings;
        document.getElementById("status").textContent = data.status;
    } catch (error) {
        console.error("Error cargando métricas:", error);
    }
}

async function loadReadings() {
    try {
        const response = await fetch("/api/readings");
        const data = await response.json();

        const table = document.getElementById("readingsTable");
        table.innerHTML = "";

        data.forEach(reading => {
            const row = document.createElement("tr");

            /*
                Se obtiene el sensor_id teniendo en cuenta dos posibles formatos:
                - sensor_id: cuando viene desde la base de datos/PostgreSQL.
                - sensorId: cuando viene directamente desde JavaScript o Java.
            */
            const sensorId = reading.sensor_id || reading.sensorId || "";

            /*
                Se convierte a minúscula para reconocer lecturas manuales
                sin importar si se escriben como:
                MANUAL-sensor-1, manual-sensor-1, Manual-sensor-1, etc.
            */
            const sensorIdLower = sensorId.toLowerCase();

            const isManualReading =
                sensorIdLower.startsWith("manual") ||
                sensorIdLower.includes("manual");

            const statusClass = reading.status === "NORMAL"
                ? "status-normal"
                : "status-alert";

            row.innerHTML = `
                <td>${reading.node_id || reading.nodeId || "---"}</td>
                <td>${sensorId}</td>
                <td>${reading.temperature} °C</td>
                <td>${reading.humidity} %</td>
                <td class="${statusClass}">${reading.status}</td>
                <td>${reading.processed_by || reading.processedBy || "---"}</td>
                <td>${new Date(reading.timestamp).toLocaleString()}</td>
            `;

            /*
                Si la lectura es manual, se aplica una clase CSS y además
                se refuerza el estilo directamente en las celdas.
            */
            if (isManualReading) {
                row.classList.add("manual-reading");

                row.querySelectorAll("td").forEach(cell => {
                    cell.style.backgroundColor = "rgba(232, 184, 75, 0.28)";
                    cell.style.fontWeight = "700";
                    cell.style.textDecoration = "underline";
                    cell.style.textDecorationThickness = "2px";
                    cell.style.textUnderlineOffset = "4px";
                });
            }

            table.appendChild(row);
        });
    } catch (error) {
        console.error("Error cargando lecturas:", error);
    }
}

async function sendManualReading() {
    const nodeId = document.getElementById("manualNodeId").value.trim();
    const sensorIdInput = document.getElementById("manualSensorId").value.trim();
    const temperatureInput = document.getElementById("manualTemperature").value;
    const humidityInput = document.getElementById("manualHumidity").value;

    const temperature = Number(temperatureInput);
    const humidity = Number(humidityInput);

    if (!nodeId || !sensorIdInput || temperatureInput === "" || humidityInput === "") {
        showManualMessage("Completa todos los campos antes de guardar.", false);
        return;
    }

    if (Number.isNaN(temperature) || Number.isNaN(humidity)) {
        showManualMessage("Temperatura y humedad deben ser valores numéricos.", false);
        return;
    }

    if (temperature < -10 || temperature > 60) {
        showManualMessage("La temperatura debe estar entre -10 °C y 60 °C.", false);
        return;
    }

    if (humidity < 0 || humidity > 100) {
        showManualMessage("La humedad debe estar entre 0% y 100%.", false);
        return;
    }

    /*
        Se revisa el sensor en minúscula para evitar duplicar el prefijo.
        Si el usuario escribe:
        - sensor-1        -> se guarda como MANUAL-sensor-1
        - manual-sensor-1 -> se guarda igual
        - MANUAL-sensor-1 -> se guarda igual
        - Manual-sensor-1 -> se guarda igual
    */
    const sensorIdLower = sensorIdInput.toLowerCase();

    const sensorId = sensorIdLower.startsWith("manual")
        ? sensorIdInput
        : `MANUAL-${sensorIdInput}`;

    const reading = {
        nodeId: nodeId,
        sensorId: sensorId,
        temperature: temperature,
        humidity: humidity,
        timestamp: new Date().toISOString()
    };

    try {
        const response = await fetch("/api/readings", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(reading)
        });

        if (!response.ok) {
            throw new Error("Error al guardar la lectura manual");
        }

        const data = await response.json();

        showManualMessage(
            `Lectura manual guardada correctamente. Procesada por ${data.processedBy || data.processed_by || "el sistema"}.`,
            true
        );

        document.getElementById("manualSensorId").value = "";
        document.getElementById("manualTemperature").value = "";
        document.getElementById("manualHumidity").value = "";

        await loadDashboard();
    } catch (error) {
        console.error(error);
        showManualMessage("No se pudo guardar la lectura manual.", false);
    }
}

function clearManualForm() {
    document.getElementById("manualNodeId").value = "";
    document.getElementById("manualSensorId").value = "";
    document.getElementById("manualTemperature").value = "";
    document.getElementById("manualHumidity").value = "";
    showManualMessage("", true);
}

function showManualMessage(text, success) {
    const message = document.getElementById("manualMessage");
    message.textContent = text;

    if (!text) {
        message.className = "";
        return;
    }

    message.className = success ? "message-ok" : "message-error";
}

setInterval(loadDashboard, 3000);
loadDashboard();