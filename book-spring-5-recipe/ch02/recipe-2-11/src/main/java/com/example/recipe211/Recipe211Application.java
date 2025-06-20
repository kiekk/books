package com.example.recipe211;

import com.example.recipe211.shop.Cashier;
import com.example.recipe211.shop.Product;
import com.example.recipe211.shop.ShoppingCart;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class Recipe211Application {

    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // profile 설정
        // 1) setActiveProfiles() 사용
//        context.getEnvironment().setActiveProfiles("global", "winter");
        // 2) vm option
        // -Dspring.profiles.active = ${profile}

        // default profile 설정
        // 1) setDefaultProfiles() 사용
//        context.getEnvironment().setDefaultProfiles("global", "winter");
        // 2) vm option
        // -Dspring.profiles.default = ${profile}
        context.scan("com.example.recipe211.shop");
        context.refresh();

        Product aaa = context.getBean("aaa", Product.class);
        Product cdrw = context.getBean("cdrw", Product.class);
        Product dvdrw = context.getBean("dvdrw", Product.class);

        ShoppingCart cart1 = context.getBean("shoppingCart", ShoppingCart.class);
        cart1.addItem(aaa);
        cart1.addItem(cdrw);
        System.out.println("Shopping cart 1 contains " + cart1.getItems());

        ShoppingCart cart2 = context.getBean("shoppingCart", ShoppingCart.class);
        cart2.addItem(dvdrw);
        System.out.println("Shopping cart 2 contains " + cart2.getItems());

        Cashier cashier = context.getBean("cashier", Cashier.class);
        cashier.checkout(cart1);
        cashier.checkout(cart2);
    }

}
