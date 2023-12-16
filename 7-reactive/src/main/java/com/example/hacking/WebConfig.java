package com.example.hacking;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
