package com.IoT.java_backend.model;


    public class SensorReading {

        private String nodeId;
        private String sensorId;
        private Double temperature;
        private Double humidity;
        private String timestamp;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getSensorId() {
            return sensorId;
        }

        public void setSensorId(String sensorId) {
            this.sensorId = sensorId;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getHumidity() {
            return humidity;
        }

        public void setHumidity(Double humidity) {
            this.humidity = humidity;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

