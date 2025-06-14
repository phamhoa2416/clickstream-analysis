package com.example.clickstream.service;

import com.example.clickstream.models.Event;
import com.example.clickstream.producer.ClickstreamProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClickstreamService {
    private final ClickstreamProducer producer;

    public ClickstreamService(ClickstreamProducer producer)
 {
        this.producer = producer;
    }

    public void processEvents(List<Map<String, Object>> rawEvents) {
        List<Event> events = rawEvents.stream()
                .map(this::convertToEvent)
                .collect(Collectors.toList());

        producer.sendEvents(events);
    }

    private Event convertToEvent(Map<String, Object> rawEvent) {
        Event event = new Event();

        event.setEventId((String) rawEvent.get("event_id"));
        event.setEventName((String) rawEvent.get("event_name"));
        event.setEventTimestamp(Instant.parse((String) rawEvent.get("event_time")));
        event.setUserId((String) rawEvent.get("user_id"));
        event.setSessionId((String) rawEvent.get("session_id"));
        event.setAppId((String) rawEvent.get("app_id"));
        event.setPlatform((String) rawEvent.get("platform"));
        event.setPageUrl((String) rawEvent.get("page_url"));

        Map<String, String> eventParams = rawEvent.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toString()
                ));
        event.setEventParams(eventParams);

        return event;
    }
}
