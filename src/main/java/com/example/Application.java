package com.example;

import com.example.clickstream.config.AppConfig;
import com.example.clickstream.processor.ClickstreamProcessor;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        // Start Spring Boot application

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .run(args)) {
            SparkSession sparkSession = context.getBean(SparkSession.class);
            AppConfig appConfig = context.getBean(AppConfig.class);
            ClickstreamProcessor processor = new ClickstreamProcessor(sparkSession, appConfig);
            processor.startProcessing();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}