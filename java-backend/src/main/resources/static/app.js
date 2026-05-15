let currentNodeId = "";

async function loadDashboard() {
    await loadMetrics();
    await loadReadings();
}

async function loadMetrics() {
    try {
        const response = await fetch("/api/metrics");

        if (!response.ok) {
            throw new Error("Error cargando métricas");
        }

        const data = await response.json();

        currentNodeId = data.nodeId;

        document.getElementById("nodeId").textContent = data.nodeId || "---";
        document.getElementById("totalReadings").textContent = data.totalReadings ?? 0;
        document.getElementById("status").textContent = data.status || "---";

        configureManualForm(data.nodeId);

    } catch (error) {
        console.error("Error cargando métricas:", error);
        document.getElementById("status").textContent = "ERROR";
    }
}

function configureManualForm(nodeId) {
    const nodeInput = document.getElementById("manualNodeId");
    const sensorSelect = document.getElementById("manualSensorId");

    if (!nodeInput || !sensorSelect || !nodeId) {
        return;
    }

    /*
        El nodo origen queda fijo según la máquina actual.
        Ejemplo:
        - Caren: machine-1
        - Fredy: machine-2
        - André: machine-3
    */
    nodeInput.value = nodeId;
    nodeInput.readOnly = true;

    /*
        Evita recargar el desplegable cada 3 segundos.
    */
    if (sensorSelect.dataset.loadedFor === nodeId) {
        return;
    }

    sensorSelect.innerHTML = "";

    const defaultOption = document.createElement("option");
    defaultOption.value = "";
    defaultOption.textContent = "Seleccione un sensor";
    sensorSelect.appendChild(defaultOption);

    /*
        Sensores manuales disponibles.
        Estos salen iguales en todas las máquinas.
    */
    for (let i = 1; i <= 5; i++) {
        const option = document.createElement("option");
        option.value = `manual-sensor-${i}`;
        option.textContent = `manual-sensor-${i}`;
        sensorSelect.appendChild(option);
    }

    sensorSelect.dataset.loadedFor = nodeId;
}

async function loadReadings() {
    try {
        const response = await fetch("/api/readings");

        if (!response.ok) {
            throw new Error("Error cargando lecturas");
        }

        const data = await response.json();

        const table = document.getElementById("readingsTable");
        table.innerHTML = "";

        data.forEach(reading => {
            const row = document.createElement("tr");

            /*
                Se obtiene el sensor_id teniendo en cuenta dos posibles formatos:
                - sensor_id: cuando viene desde PostgreSQL.
                - sensorId: cuando viene desde Java/JavaScript.
            */
            const sensorId = reading.sensor_id || reading.sensorId || "";

            /*
                Reconoce lecturas manuales sin importar mayúsculas/minúsculas.
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
                Si la lectura es manual, se resalta en la tabla.
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
    const nodeId = currentNodeId || document.getElementById("manualNodeId").value.trim();
    const sensorId = document.getElementById("manualSensorId").value;
    const temperatureInput = document.getElementById("manualTemperature").value;
    const humidityInput = document.getElementById("manualHumidity").value;

    const temperature = Number(temperatureInput);
    const humidity = Number(humidityInput);

    if (!nodeId) {
        showManualMessage("No se pudo identificar el nodo actual.", false);
        return;
    }

    if (!sensorId) {
        showManualMessage("Seleccione un sensor antes de guardar.", false);
        return;
    }

    if (temperatureInput === "" || humidityInput === "") {
        showManualMessage("Completa temperatura y humedad antes de guardar.", false);
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
    document.getElementById("manualSensorId").value = "";
    document.getElementById("manualTemperature").value = "";
    document.getElementById("manualHumidity").value = "";

    if (currentNodeId) {
        document.getElementById("manualNodeId").value = currentNodeId;
    }

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