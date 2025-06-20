package com.example.recipe1236.nosql;

import org.neo4j.driver.Driver;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableNeo4jRepositories
@ComponentScan
public class StarwarsConfig {

    @Bean
    public SessionFactory sessionFactory() {
        return new SessionFactory("com.apress.springrecipes.nosql");
    }

    @Bean
    public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new Neo4jTransactionManager((Driver) sessionFactory);
    }

}
