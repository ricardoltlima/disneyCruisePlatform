package com.disney.app.reservationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaProperties {

    private String bootstrapServers;
    private final Topics topics = new Topics();
    private final Producer producer = new Producer();
    private final Schemas schemas = new Schemas();

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Topics getTopics() {
        return topics;
    }

    public Producer getProducer() {
        return producer;
    }

    public Schemas getSchemas() {
        return schemas;
    }

    public static class Topics {
        private String reservationCreated;

        public String getReservationCreated() {
            return reservationCreated;
        }

        public void setReservationCreated(String reservationCreated) {
            this.reservationCreated = reservationCreated;
        }
    }

    public static class Producer {
        private String acks;
        private String clientId;

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class Schemas {
        private String reservationCreated;

        public String getReservationCreated() {
            return reservationCreated;
        }

        public void setReservationCreated(String reservationCreated) {
            this.reservationCreated = reservationCreated;
        }
    }
}
