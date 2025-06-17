package com.example.clickstream.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class ClickHouseConfig {
    private final AppConfig appConfig;

    @Autowired
    public ClickHouseConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public DataSource clickHouseDataSource() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", appConfig.getClickhouse().getUser());
        props.setProperty("password", appConfig.getClickhouse().getPassword());
        props.setProperty("socketTimeout", "30000");

        return new ClickHouseDataSource(
                appConfig.getClickhouse().getUrl(),
                props
        );
    }

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate() throws SQLException {
        return new JdbcTemplate(clickHouseDataSource());
    }
}
