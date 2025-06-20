package com.example.recipe392.court.config;

import com.example.recipe392.court.domain.SportTypeConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BindingConfiguration implements WebMvcConfigurer {

    private final SportTypeConverter sportTypeConverter;

    public BindingConfiguration(SportTypeConverter sportTypeConverter) {
        this.sportTypeConverter = sportTypeConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(this.sportTypeConverter);
    }
}
