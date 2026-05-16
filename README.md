# Plataforma IoT Agrícola Distribuida

Este proyecto implementa una plataforma distribuida para el monitoreo de sensores agrícolas de temperatura y humedad en tiempo real.

El sistema permite registrar lecturas automáticas generadas por simuladores Node.js y lecturas manuales registradas desde un dashboard web. Cada computador ejecuta su propio backend Java Spring Boot, su dashboard y su simulador de sensores. Todas las lecturas se almacenan en una base de datos PostgreSQL central ubicada en Machine-1 / Caren.

---

## Objetivo del proyecto

Desarrollar una solución distribuida capaz de recibir, procesar, almacenar y visualizar lecturas de sensores agrícolas desde varios computadores conectados en red local.

El sistema permite:

- Registrar lecturas automáticas de sensores.
- Registrar lecturas manuales desde el dashboard.
- Procesar datos desde diferentes nodos.
- Guardar toda la información en una base de datos central.
- Visualizar los datos en tiempo casi real.
- Filtrar lecturas por nodo, sensor y fecha.
- Consultar datos históricos almacenados en PostgreSQL.

---

## Tecnologías utilizadas

| Tecnología | Uso dentro del proyecto |
|---|---|
| Java 21 | Desarrollo del backend |
| Spring Boot | Creación de la API REST |
| PostgreSQL | Base de datos central |
| Node.js | Simulación de sensores automáticos |
| HTML, CSS y JavaScript | Dashboard web |
| Git y GitHub | Control de versiones |
| IntelliJ IDEA | Entorno de desarrollo |
| PowerShell | Ejecución de comandos y pruebas de red |

---

## Arquitectura del sistema

El sistema está distribuido en cuatro computadores conectados a una red local.

| Máquina | IP | Componentes |
|---|---|---|
| Machine-1 / Caren | 172.20.10.7 | Backend Java + Dashboard + Node.js + PostgreSQL |
| Machine-2 / Fredy | 172.20.10.2 | Backend Java + Dashboard + Node.js |
| Machine-3 / André | 172.20.10.3 | Backend Java + Dashboard + Node.js |
| Machine-4 / Samuk | 172.20.10.5 | Backend Java + Dashboard + Node.js |

La base de datos PostgreSQL está ubicada en Machine-1 / Caren. Las demás máquinas se conectan a esa base de datos mediante la red local.

```text
Machine-1 / Caren
PostgreSQL + Backend Java + Dashboard + Simulador Node.js

Machine-2 / Fredy
Backend Java + Dashboard + Simulador Node.js

Machine-3 / André
Backend Java + Dashboard + Simulador Node.js

Machine-4 / Samuk
Backend Java + Dashboard + Simulador Node.js
```

---

## Flujo de funcionamiento

### Lecturas automáticas

Cada máquina ejecuta un simulador Node.js que genera lecturas de temperatura y humedad cada 14 segundos.

```text
Simulador Node.js
        ↓
Backend Java local
        ↓
PostgreSQL central en Machine-1
        ↓
Dashboard web
```

### Lecturas manuales

Cada usuario puede registrar lecturas manuales desde el dashboard de su propia máquina.

```text
Dashboard web
        ↓
Backend Java local
        ↓
PostgreSQL central en Machine-1
        ↓
Visualización en todos los dashboards
```

Cada lectura queda asociada al nodo de origen y al backend que la procesó.

Ejemplo de nodos:

```text
machine-1
machine-2
machine-3
machine-4
```

---

## Requisitos de instalación

Cada máquina debe tener instalado:

- Git
- Java JDK 21
- IntelliJ IDEA Community
- Node.js LTS
- npm

Solo Machine-1 / Caren necesita PostgreSQL instalado, porque allí se almacena la base de datos central.

---

## Instalación de herramientas en Windows

### Instalar Git

```powershell
winget install --id Git.Git -e --source winget
```

Verificar instalación:

```powershell
git --version
```

### Instalar Java JDK 21

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget
```

Verificar instalación:

```powershell
java -version
javac -version
```

### Instalar IntelliJ IDEA Community

```powershell
winget install --id JetBrains.IntelliJIDEA.Community -e --source winget
```

### Instalar Node.js LTS

```powershell
winget install --id OpenJS.NodeJS.LTS -e --source winget
```

Verificar instalación:

```powershell
node -v
npm -v
```

---

## Clonar el proyecto

En cada máquina se debe clonar el repositorio:

```powershell
cd C:\Users\TU_USUARIO
git clone https://github.com/Carencitha/iot-agro-distribuido.git
cd iot-agro-distribuido
```

---

## Configuración de PostgreSQL en Machine-1

La base de datos central se encuentra en Machine-1 / Caren.

Datos principales:

```text
IP Machine-1: 172.20.10.7
Puerto PostgreSQL: 5432
Base de datos: iot_agro
Usuario: postgres
Contraseña: 1234
```

Crear la base de datos:

```sql
CREATE DATABASE iot_agro;
```

Crear la tabla principal:

```sql
CREATE TABLE sensor_readings (
    id SERIAL PRIMARY KEY,
    node_id VARCHAR(50) NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    humidity DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_by VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Permitir conexiones externas en PostgreSQL

En Machine-1 / Caren se debe editar el archivo:

```text
C:\Program Files\PostgreSQL\18\data\postgresql.conf
```

Buscar:

```conf
#listen_addresses = 'localhost'
```

Cambiar por:

```conf
listen_addresses = '*'
```

Luego editar:

```text
C:\Program Files\PostgreSQL\18\data\pg_hba.conf
```

Agregar la red local:

```conf
host    iot_agro    postgres    172.20.10.0/24    scram-sha-256
```

Reiniciar PostgreSQL:

```powershell
Restart-Service postgresql-x64-18
```

---

## Abrir puertos en el firewall

En Machine-1 permitir PostgreSQL:

```powershell
New-NetFirewallRule -DisplayName "PostgreSQL 5432" -Direction Inbound -Protocol TCP -LocalPort 5432 -Action Allow
```

En todas las máquinas permitir Spring Boot:

```powershell
New-NetFirewallRule -DisplayName "Spring Boot 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

---

## Configuración del backend Java

El archivo de configuración se encuentra en:

```text
java-backend/src/main/resources/application.properties
```

### Machine-1 / Caren

```properties
spring.application.name=java-backend

server.port=8080
server.address=0.0.0.0

spring.datasource.url=jdbc:postgresql://localhost:5432/iot_agro
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver

app.node-id=machine-1
app.processing-threads=8
```

### Machine-2 / Fredy

```properties
spring.application.name=java-backend

server.port=8080
server.address=0.0.0.0

spring.datasource.url=jdbc:postgresql://172.20.10.7:5432/iot_agro
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver

app.node-id=machine-2
app.processing-threads=8
```

### Machine-3 / André

```properties
spring.application.name=java-backend

server.port=8080
server.address=0.0.0.0

spring.datasource.url=jdbc:postgresql://172.20.10.7:5432/iot_agro
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver

app.node-id=machine-3
app.processing-threads=8
```

### Machine-4 / Samuk

```properties
spring.application.name=java-backend

server.port=8080
server.address=0.0.0.0

spring.datasource.url=jdbc:postgresql://172.20.10.7:5432/iot_agro
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver

app.node-id=machine-4
app.processing-threads=8
```

---

## Ejecutar el backend Java

Abrir IntelliJ IDEA y seleccionar:

```text
File > Open
```

Abrir la carpeta:

```text
iot-agro-distribuido/java-backend
```

Ejecutar la clase principal:

```text
JavaBackendApplication
```

Cuando el backend esté funcionando, debe aparecer algo parecido a:

```text
Tomcat started on port 8080
Started JavaBackendApplication
```

---

## Probar el backend

En cada máquina abrir:

```text
http://localhost:8080/api/metrics
```

Respuesta esperada:

```json
{
  "nodeId": "machine-1",
  "totalReadings": 100,
  "status": "ONLINE"
}
```

El valor de `nodeId` cambia según la máquina.

---

## Configuración del simulador Node.js

El archivo `.env` se encuentra en:

```text
sensor-simulator/.env
```

### Machine-1

```env
NODE_ID=machine-1
SENSOR_COUNT=5
INTERVAL_MS=14000
TARGET_NODES=http://localhost:8080/api/readings
```

### Machine-2

```env
NODE_ID=machine-2
SENSOR_COUNT=5
INTERVAL_MS=14000
TARGET_NODES=http://localhost:8080/api/readings
```

### Machine-3

```env
NODE_ID=machine-3
SENSOR_COUNT=5
INTERVAL_MS=14000
TARGET_NODES=http://localhost:8080/api/readings
```

### Machine-4

```env
NODE_ID=machine-4
SENSOR_COUNT=5
INTERVAL_MS=14000
TARGET_NODES=http://localhost:8080/api/readings
```

---

## Ejecutar el simulador Node.js

En cada máquina:

```powershell
cd C:\Users\TU_USUARIO\iot-agro-distribuido\sensor-simulator
npm install
node index.js
```

Salida esperada:

```text
Intervalo: 14000 ms
Destinos:
- http://localhost:8080/api/readings

[OK] machine-2-sensor-1 enviado a http://localhost:8080/api/readings | Procesado por: machine-2
```

Para detener el simulador:

```text
Ctrl + C
```

---

## Ejecutar el dashboard

Con el backend activo, abrir en el navegador:

```text
http://localhost:8080/
```

El dashboard permite:

- Ver el nodo actual.
- Ver el total de lecturas.
- Registrar lecturas manuales.
- Visualizar lecturas automáticas.
- Filtrar datos por nodo.
- Filtrar datos por sensor.
- Filtrar datos por fecha.
- Consultar datos históricos.

---

## Filtros del dashboard

El dashboard incluye filtros por:

### Nodo

```text
machine-1
machine-2
machine-3
machine-4
```

### Sensor

```text
sensor-1
sensor-2
sensor-3
sensor-4
sensor-5
Lecturas manuales
```

### Fecha

Permite consultar datos históricos guardados en PostgreSQL.

Ejemplo:

```text
Nodo: machine-3
Sensor: sensor-2
Fecha: 2026-05-15
```

Esto muestra únicamente las lecturas del sensor 2 de Machine-3 en esa fecha.

---

## Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/metrics` | Muestra métricas del nodo |
| GET | `/api/readings` | Muestra las últimas lecturas |
| GET | `/api/readings?nodeId=machine-2` | Filtra por nodo |
| GET | `/api/readings?sensor=sensor-1` | Filtra por sensor |
| GET | `/api/readings?date=2026-05-15` | Filtra por fecha |
| GET | `/api/readings?nodeId=machine-3&sensor=sensor-2&date=2026-05-15` | Filtro combinado |
| POST | `/api/readings` | Registra una nueva lectura |
| GET | `/api/health` | Verifica el estado del backend |

---

## Ejemplo de lectura enviada por POST

```json
{
  "nodeId": "machine-2",
  "sensorId": "machine-2-sensor-1",
  "temperature": 28.5,
  "humidity": 70.2,
  "timestamp": "2026-05-15T10:30:00Z"
}
```

---

## Pruebas recomendadas

Probar conexión a PostgreSQL desde otra máquina:

```powershell
Test-NetConnection 172.20.10.7 -Port 5432
```

Resultado esperado:

```text
TcpTestSucceeded : True
```

Probar backend:

```text
http://localhost:8080/api/metrics
```

Probar dashboard:

```text
http://localhost:8080/
```

Probar sensores automáticos:

```powershell
node index.js
```

---

## Consultas útiles en PostgreSQL

Ver total de lecturas por nodo:

```sql
SELECT node_id, COUNT(*) AS total
FROM sensor_readings
GROUP BY node_id
ORDER BY node_id;
```

Ver lecturas por fecha:

```sql
SELECT timestamp::date AS fecha, node_id, COUNT(*) AS total
FROM sensor_readings
GROUP BY timestamp::date, node_id
ORDER BY fecha DESC, node_id;
```

Ver últimas lecturas:

```sql
SELECT *
FROM sensor_readings
ORDER BY created_at DESC
LIMIT 20;
```

---

## Tolerancia a fallos

El sistema tiene tolerancia parcial a fallos:

- Si se apaga Machine-2, Machine-3 o Machine-4, las demás máquinas pueden seguir funcionando.
- Si se apaga Machine-1, la base PostgreSQL central queda temporalmente no disponible.
- Como mejora futura se propone implementar una cola local temporal para guardar lecturas pendientes y sincronizarlas cuando Machine-1 vuelva a estar activa.

---

## Mejora futura

Una mejora futura sería implementar MQTT para que los sensores publiquen datos en un broker, en lugar de enviarlos directamente por HTTP al backend.

```text
Sensores Node.js
        ↓
Broker MQTT
        ↓
Backend Java
        ↓
PostgreSQL
        ↓
Dashboard
```

---

## Integrantes

- Caren
- Fredy
- André
  




