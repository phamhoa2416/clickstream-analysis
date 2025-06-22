package com.example.clickstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.*;

public class FakeEventProducer {
    private static final String TOPIC = "clickstream-events-100k";
    private static final String BOOTSTRAP_SERVERS = "localhost:29092";
    private static final int NUM_EVENTS = 100_000;

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        Random random = new Random();
        ObjectMapper mapper = new ObjectMapper();

        String[] eventNames = {"click", "view", "scroll", "purchase", "add_to_cart"};
        String[] deviceTypes = {"mobile", "desktop", "tablet"};
        String[] deviceBrands = {"Apple", "Samsung", "Huawei", "Xiaomi", "Dell"};
        String[] osNames = {"iOS", "Android", "Windows", "macOS", "Linux"};
        String[] browsers = {"Chrome", "Safari", "Firefox", "Edge"};
        String[] categories = {"electronics", "clothing", "home"};
        String[] currencies = {"USD", "EUR", "JPY", "VND"};

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < NUM_EVENTS; i++) {
                String eventId = UUID.randomUUID().toString();
                String eventName = eventNames[random.nextInt(eventNames.length)];
                Instant now = Instant.now();
                String sessionId = "session_" + random.nextInt(5000);
                int sessionSeq = random.nextInt(100);

                Map<String, Object> event = new HashMap<>();
                event.put("event_id", eventId);
                event.put("event_name", eventName);
                event.put("event_time", now.toString());
                event.put("user_id", "user_" + random.nextInt(10000));
                event.put("anonymous_id", UUID.randomUUID().toString());

                event.put("session_id", sessionId);
                event.put("session_start_time", now.minusSeconds(random.nextInt(600)).toString());
                event.put("session_sequence", sessionSeq);

                event.put("app_id", "ecommerce_app");
                event.put("schema_version", "1.0.0");

                event.put("platform", "web");

                String pageUrl = "https://store.com/" + eventName + "/" + UUID.randomUUID().toString().substring(0, 5);
                event.put("page_url", pageUrl);
                event.put("page_path", pageUrl.replace("https://store.com", ""));
                event.put("page_referrer", "https://google.com?q=shop");

                event.put("utm_source", "google");
                event.put("utm_medium", "cpc");
                event.put("utm_campaign", "summer_sale");

                event.put("device_type", deviceTypes[random.nextInt(deviceTypes.length)]);
                event.put("device_brand", deviceBrands[random.nextInt(deviceBrands.length)]);
                event.put("os_name", osNames[random.nextInt(osNames.length)]);
                event.put("browser_name", browsers[random.nextInt(browsers.length)]);
                event.put("browser_version", "v" + (random.nextInt(90) + 10));
                event.put("screen_resolution", "1920x1080");
                event.put("user_agent", "Mozilla/5.0 test-agent");
                event.put("language", "en-US");

                event.put("product_id", "product_" + random.nextInt(1000));
                event.put("product_category", categories[random.nextInt(categories.length)]);
                event.put("order_id", UUID.randomUUID().toString());
                event.put("revenue", eventName.equals("purchase") ? 10 + (1000 * random.nextDouble()) : null);
                event.put("currency", currencies[random.nextInt(currencies.length)]);

                event.put("page_load_time", random.nextFloat() * 3000);
                event.put("time_to_interactive", random.nextFloat() * 5000);

                String json = mapper.writeValueAsString(event);

                producer.send(new ProducerRecord<>(TOPIC, eventId, json));

                if (i % 10_000 == 0) {
                    System.out.printf("Generated %d events%n", i);
                }
            }

            producer.flush();
            System.out.println("✔ All events produced successfully.");
        }
    }
}
