package com.example.recipe10101.bookshop.config;

import com.example.recipe10101.bookshop.BookShop;
import com.example.recipe10101.bookshop.BookShopCashier;
import com.example.recipe10101.bookshop.JdbcBookShop;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
// 로드 타임 위빙 사용
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableLoadTimeWeaving
public class BookstoreConfiguration {

    @Bean
    public DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(org.postgresql.Driver.class.getName());
        dataSource.setUrl("jdbc:postgresql://localhost:5432/bookstore");
        dataSource.setUsername("postgres");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }

    @Bean
    public JdbcBookShop bookShop(DataSource dataSource) {
        JdbcBookShop bookShop = new JdbcBookShop();
        bookShop.setDataSource(dataSource);
        return bookShop;
    }

    @Bean
    public BookShopCashier cashier(BookShop bookShop) {
        BookShopCashier cashier = new BookShopCashier();
        cashier.setBookShop(bookShop);
        return cashier;
    }
}
