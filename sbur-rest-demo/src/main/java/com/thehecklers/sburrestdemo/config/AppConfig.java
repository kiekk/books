package com.thehecklers.sburrestdemo.config;

import com.thehecklers.sburrestdemo.config.properties.DroidProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    @ConfigurationProperties("droid")
    DroidProperties createDroidProperties() {
        return new DroidProperties();
    }
}
