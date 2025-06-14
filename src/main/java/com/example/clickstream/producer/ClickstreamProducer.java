package com.example.clickstream.producer;

import com.example.clickstream.config.AppConfig;
import com.example.clickstream.models.Event;
import com.example.monitoring.metrics.ProducerMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.clickstream.exception.ProducerException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class ClickstreamProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final ProducerMetrics metrics;

    public ClickstreamProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            AppConfig appConfig,
            ObjectMapper objectMapper,
            ProducerMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Retryable(backoff = @Backoff(delay = 1000))
    public void sendEvent(Event event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("Preparing to send event: {}", objectMapper.writeValueAsString(event));
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    appConfig.getKafkaTopic(),
                    event.getEventId(),
                    eventJson
            );
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);

            SendResult<String, String> result = kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            log.info("Event sent to Kafka topic: {}, partition: {}, offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            log.debug("Event sent to Kafka: {}", event.getEventId());
            metrics.incrementSuccess();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            metrics.incrementFailure();
            log.error("Failed to send event {}", event.getEventId(), e);
        } catch (Exception e) {
            metrics.incrementFailure();
            log.error("Unexpected error sending event", e);
            throw new ProducerException("Failed to send event", e);
        }
    }

    public void sendEvents(List<Event> events) {
        events.forEach(this::sendEvent);
    }
}
