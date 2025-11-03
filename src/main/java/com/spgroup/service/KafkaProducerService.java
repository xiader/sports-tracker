package com.spgroup.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spgroup.config.KafkaClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for publishing messages to Kafka
 * Implements retry logic for transient failures
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, KafkaClientConfiguration config) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = config.getTopic();
    }

    /**
     * Sends message to Kafka with retry logic
     *
     * @param key     Message key (eventId)
     * @param message Message payload
     * @return true if successful, false otherwise
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public boolean sendMessage(String key, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(this.topic, key, jsonMessage);

            // Wait for result with timeout
            SendResult<String, String> result = future.get(10, TimeUnit.SECONDS);

            log.info("Successfully sent message to Kafka: key=[{}], topic=[{}], partition=[{}], offset=[{}]",
                    key,
                    this.topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return true;

        } catch (JsonProcessingException e) {
            log.error("Error serializing message to JSON for key=[{}]", key, e);
            return false;
        } catch (Exception e) {
            log.error("Error sending message to Kafka for key=[{}]", key, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

}
