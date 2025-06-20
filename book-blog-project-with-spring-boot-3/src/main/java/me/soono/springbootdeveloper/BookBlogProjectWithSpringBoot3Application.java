package me.soono.springbootdeveloper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "me.soono.springbootdeveloper.config.properties")
public class BookBlogProjectWithSpringBoot3Application {

    public static void main(String[] args) {
        SpringApplication.run(BookBlogProjectWithSpringBoot3Application.class, args);
    }

}
