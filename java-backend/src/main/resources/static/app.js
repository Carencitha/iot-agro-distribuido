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

            const statusClass = reading.status === "NORMAL"
                ? "status-normal"
                : "status-alert";

            row.innerHTML = `
                <td>${reading.node_id}</td>
                <td>${reading.sensor_id}</td>
                <td>${reading.temperature} °C</td>
                <td>${reading.humidity} %</td>
                <td class="${statusClass}">${reading.status}</td>
                <td>${reading.processed_by}</td>
                <td>${new Date(reading.timestamp).toLocaleString()}</td>
            `;

            table.appendChild(row);
        });
    } catch (error) {
        console.error("Error cargando lecturas:", error);
    }
}

setInterval(loadDashboard, 3000);
loadDashboard();