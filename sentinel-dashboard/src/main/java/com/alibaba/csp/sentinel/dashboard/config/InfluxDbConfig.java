package com.alibaba.csp.sentinel.dashboard.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author stelin <stelin@swoft.org>
 * @date 2021-07-01 14:24:16
 */
@Configuration
public class InfluxDbConfig {
    @Value("${spring.influx.url}")
    private String url;

    @Value("${spring.influx.user}")
    private String username;

    @Value("${spring.influx.password}")
    private String password;

    @Value("${spring.influx.database}")
    private String database;

    @Bean
    public InfluxDB influxDb() {
        InfluxDB influxDb = InfluxDBFactory.connect(url, username, password);
        influxDb.setDatabase(database).enableBatch(100, 2000, TimeUnit.MILLISECONDS);
        influxDb.setRetentionPolicy("autogen");
        influxDb.setLogLevel(InfluxDB.LogLevel.BASIC);
        return influxDb;
    }
}
