package com.example.recipe972.course.config;

import com.example.recipe972.course.CourseDao;
import com.example.recipe972.course.jpa.JpaCourseDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;

@Configuration
public class CourseConfiguration {

    @Bean
    public CourseDao courseDao(EntityManagerFactory entityManagerFactory) {
        return new JpaCourseDao(entityManagerFactory);
    }

    @Bean
    public LocalEntityManagerFactoryBean entityManagerFactory() {

        LocalEntityManagerFactoryBean emf = new LocalEntityManagerFactoryBean();
        emf.setPersistenceUnitName("course");
        return emf;
    }
}
