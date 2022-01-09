package com.example.recipe971.course.config;

import com.example.recipe971.course.Course;
import com.example.recipe971.course.CourseDao;
import com.example.recipe971.course.hibernate.HibernateCourseDao;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import java.util.Properties;

@Configuration
public class CourseConfiguration {

    @Bean
    public CourseDao courseDao(SessionFactory sessionFactory) {
        return new HibernateCourseDao(sessionFactory);
    }


    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setHibernateProperties(hibernateProperties());
        sessionFactoryBean.setAnnotatedClasses(Course.class);
        // 1. XML 매핑 파일 경로를 지정해 매핑 파일을 읽을 수 있습니다.
//        sessionFactoryBean.setMappingLocations(new ClassPathResource('...파일 경로'));

        // 2. 와일드 카드를 사용, ResourcePatternResolver 를 이용해 리소스 패턴을 리소스로 해석
//        Resource[] mappingResources = resourcePatternResolver.getResources('..파일 경로 ex) *.hbm.xml');
//        sessionFactoryBean.setMappingLocations(mappingResources);
        return sessionFactoryBean;
    }

    private Properties hibernateProperties() {

        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.URL, "jdbc:postgresql://localhost:5432/course");
        properties.setProperty(AvailableSettings.USER, "postgres");
        properties.setProperty(AvailableSettings.PASS, "password");
        properties.setProperty(AvailableSettings.DIALECT, PostgreSQL95Dialect.class.getName());
        properties.setProperty(AvailableSettings.SHOW_SQL, String.valueOf(true));
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "update");
        return properties;
    }
}
