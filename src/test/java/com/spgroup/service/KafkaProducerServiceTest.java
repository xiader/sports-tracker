package com.spgroup.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.spgroup.config.KafkaClientConfiguration;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KafkaProducerService
 */
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SendResult<String, String> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        KafkaClientConfiguration configuration = new KafkaClientConfiguration();
        configuration.setBootstrapServers("localhost:9092");
        configuration.setProducerKeySerializer("org.apache.kafka.common.serialization.StringSerializer");
        configuration.setProducerValueSerializer("org.apache.kafka.common.serialization.StringSerializer");
        configuration.setProducerAcks("all");
        configuration.setProducerRetries("3");
        configuration.setTopic("test-topic");
        configuration.setRetryAttempts(3);
        configuration.setPartitions(1);
        configuration.setReplicas(1);

        ObjectMapper objectMapper = new ObjectMapper();
        kafkaProducerService = new KafkaProducerService(kafkaTemplate, objectMapper, configuration);
    }

    @Test
    void testSendMessageSuccess() {
        // Given
        String key = "event123";
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", key);
        message.put("score", "2:1");

        // Mock RecordMetadata to return valid partition and offset
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(100L);

        // Mock SendResult to return the RecordMetadata
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(future);

        // When
        boolean result = kafkaProducerService.sendMessage(key, message);

        // Then
        assertTrue(result);
        verify(kafkaTemplate, times(1)).send(eq("test-topic"), eq(key), anyString());
    }

    @Test
    void testSendMessageFailure() {
        // Given
        String key = "event123";
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", key);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            kafkaProducerService.sendMessage(key, message);
        });
    }

}
