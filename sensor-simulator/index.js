const axios = require("axios");
require("dotenv").config();

const NODE_ID = process.env.NODE_ID || "machine-unknown";
const SENSOR_COUNT = Number(process.env.SENSOR_COUNT || 5);
const INTERVAL_MS = Number(process.env.INTERVAL_MS || 2000);

const TARGET_NODES = process.env.TARGET_NODES
    ? process.env.TARGET_NODES.split(",")
    : ["http://localhost:8080/api/readings"];

let currentTargetIndex = 0;

function randomBetween(min, max) {
    return Number((Math.random() * (max - min) + min).toFixed(2));
}

function generateReading(sensorNumber) {
    return {
        nodeId: NODE_ID,
        sensorId: `${NODE_ID}-sensor-${sensorNumber}`,
        temperature: randomBetween(18, 40),
        humidity: randomBetween(35, 95),
        timestamp: new Date().toISOString()
    };
}

function getNextTarget() {
    const target = TARGET_NODES[currentTargetIndex];
    currentTargetIndex = (currentTargetIndex + 1) % TARGET_NODES.length;
    return target;
}

async function sendReading(reading) {
    const targetUrl = getNextTarget();

    try {
        const response = await axios.post(targetUrl, reading);

        console.log(
            `[OK] ${reading.sensorId} enviado a ${targetUrl} | Procesado por: ${response.data.processedBy}`
        );
    } catch (error) {
        console.error(
            `[ERROR] No se pudo enviar a ${targetUrl}: ${error.message}`
        );
    }
}

function startSimulation() {
    console.log("====================================");
    console.log(" Simulador de sensores IoT iniciado ");
    console.log("====================================");
    console.log("Nodo generador:", NODE_ID);
    console.log("Sensores simulados:", SENSOR_COUNT);
    console.log("Intervalo:", INTERVAL_MS, "ms");
    console.log("Destinos:");
    TARGET_NODES.forEach(target => console.log("-", target));

    setInterval(() => {
        for (let i = 1; i <= SENSOR_COUNT; i++) {
            const reading = generateReading(i);
            sendReading(reading);
        }
    }, INTERVAL_MS);
}

startSimulation();