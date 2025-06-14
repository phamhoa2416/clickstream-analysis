package com.example.clickstream.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.time.Instant;
import java.util.*;

public class FakeEventProducer {
    private static final String TOPIC = "clickstream-events-1k";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int NUM_EVENTS = 1000;

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        String[] eventNames = {"click", "view", "scroll", "purchase", "add_to_cart"};
        String[] pages = {"/home", "/product", "/cart", "/checkout", "/confirmation"};
        String[] platforms = {"web", "mobile", "android", "ios", "tablet"};
        String[] countries = {"VN", "US", "RU", "CN", "JP", "FR"};
        String[] cities = {"Hà Nội", "Thành phố Hồ Chí Minh", "Moscow", "Paris", "New York"};

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            Random random = new Random();

            for (int i = 0; i < NUM_EVENTS; i++) {
                String userId = "user_" + (random.nextInt(100) + 1);
                String sessionId = "session_" + (random.nextInt(50) + 1);
                String eventName = eventNames[random.nextInt(eventNames.length)];

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("event_id", UUID.randomUUID().toString());
                eventData.put("event_name", eventName);
                eventData.put("event_time", Instant.now().toString());
                eventData.put("user_id", userId);
                eventData.put("session_id", sessionId);
                eventData.put("app_id", "ecommerce_app");
                eventData.put("platform", platforms[random.nextInt(platforms.length)]);
                eventData.put("page_url", "https://example.com" + pages[random.nextInt(pages.length)]);
                eventData.put("geo_country", countries[random.nextInt(countries.length)]);
                eventData.put("geo_region", "Region_" + (random.nextInt(10) + 1));
                eventData.put("geo_city", cities[random.nextInt(cities.length)]);
                eventData.put("traffic_source", random.nextBoolean() ? "google" : "facebook");
                eventData.put("traffic_medium", random.nextBoolean() ? "organic" : "cpc");

                if (eventName.equals("purchase") || eventName.equals("add_to_cart")) {
                    eventData.put("item_id", "item_" + (random.nextInt(20) + 1));
                    eventData.put("item_price", random.nextDouble() * 100);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                String eventDataJson = objectMapper.writeValueAsString(eventData);

                ProducerRecord<String, String> record = new ProducerRecord<>(
                        TOPIC,
                        userId,
                        eventDataJson
                );

                final int eventIndex = i;
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("Error sending message: " + exception.getMessage());
                    } else {
                        System.out.printf("Sent event %d/%d to partition %d%n",
                                eventIndex + 1, NUM_EVENTS, metadata.partition());
                    }
                });

                Thread.sleep(random.nextInt(100));
            }

            producer.flush();
            System.out.println("All events sent successfully.");
        } catch (InterruptedException e) {
            System.err.println("Error producing events: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        } finally {
            System.out.println("Producer closed.");
        }
    }
}
