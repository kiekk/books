package com.example.recipe1042;

import com.example.recipe1042.bookshop.BookShop;
import com.example.recipe1042.bookshop.config.BookstoreConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe1042Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(BookstoreConfiguration.class);

        BookShop bookShop = context.getBean(BookShop.class);
        bookShop.purchase("0001", "user1");
    }

}
