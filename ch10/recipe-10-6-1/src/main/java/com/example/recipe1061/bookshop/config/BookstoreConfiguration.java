package com.example.recipe1061.bookshop.config;

import com.example.recipe1061.bookshop.BookShop;
import com.example.recipe1061.bookshop.BookShopCashier;
import com.example.recipe1061.bookshop.Cashier;
import com.example.recipe1061.bookshop.JdbcBookShop;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement()
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
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

    @Bean
    public JdbcBookShop bookShop(DataSource dataSource) {
        JdbcBookShop bookShop = new JdbcBookShop();
        bookShop.setDataSource(dataSource);
        return bookShop;
    }

    @Bean
    public Cashier cashier(BookShop bookShop) {
        BookShopCashier cashier = new BookShopCashier();
        cashier.setBookShop(bookShop);
        return cashier;
    }
}
