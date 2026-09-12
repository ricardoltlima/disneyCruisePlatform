package com.disney.app.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaProperties {

    private String bootstrapServers;
    private final Consumer consumer = new Consumer();
    private final Topics topics = new Topics();
    private final Schemas schemas = new Schemas();

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public Topics getTopics() {
        return topics;
    }

    public Schemas getSchemas() {
        return schemas;
    }

    public static class Consumer {
        private String groupId;
        private String autoOffsetReset;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getAutoOffsetReset() {
            return autoOffsetReset;
        }

        public void setAutoOffsetReset(String autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
        }
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
