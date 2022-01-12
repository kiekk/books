package com.example.recipe1062;

import com.example.recipe1062.bookshop.Cashier;
import com.example.recipe1062.bookshop.config.BookstoreConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class Recipe1062Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(BookstoreConfiguration.class);

        Cashier cashier = context.getBean(Cashier.class);
        List<String> isbnList = Arrays.asList("0001", "0002");
        cashier.checkout(isbnList, "user1");
    }

}
