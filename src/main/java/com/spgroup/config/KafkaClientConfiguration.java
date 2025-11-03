package com.spgroup.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaClientConfiguration {

    @Value(value = "${kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value(value = "${kafka.producer.key-serializer}")
    private String producerKeySerializer;
    @Value(value = "${kafka.producer.value-serializer}")
    private String producerValueSerializer;
    @Value(value = "${kafka.producer.acks}")
    private String producerAcks;
    @Value(value = "${kafka.producer.retries}")
    private String producerRetries;

    @Value(value = "${kafka.topic}")
    private String topic;
    @Value(value = "${kafka.retry-attempts}")
    private Integer retryAttempts;
    @Value(value = "${kafka.partitions}")
    private Integer partitions;
    @Value(value = "${kafka.replicas}")
    private Integer replicas;
}
