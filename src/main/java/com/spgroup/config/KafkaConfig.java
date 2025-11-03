package com.spgroup.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    /**
     * Creates the Kafka topic for sports events.
     * Topic is created automatically when application starts.
     */
    @Bean
    public NewTopic sportEventsTopic(KafkaClientConfiguration config) {
        return TopicBuilder.name(config.getTopic())
                .partitions(config.getPartitions())
                .replicas(config.getReplicas())
                .build();
    }

    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaClientConfiguration config) {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getProducerKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getProducerValueSerializer());

        props.put(ProducerConfig.ACKS_CONFIG, config.getProducerAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, config.getProducerRetries());

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
