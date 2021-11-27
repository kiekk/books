package com.example.recipe2103.shop.config;

import com.example.recipe2103.shop.Battery;
import com.example.recipe2103.shop.Disc;
import com.example.recipe2103.shop.DiscountFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.example.recipe2103.shop")
public class ShopConfiguration {

    @Bean
    public Battery aaa() {
        Battery aaa = new Battery("AAA", 2.5);
        return aaa;
    }

    @Bean
    public Disc cdrw() {
        Disc aaa = new Disc("CD-RW", 1.5);
        return aaa;
    }

    @Bean
    public Disc dvdrw() {
        Disc aaa = new Disc("DVD-RW", 3.0);
        return aaa;
    }

    @Bean
    public DiscountFactoryBean discountFactoryBeanAAA() {
        DiscountFactoryBean factory = new DiscountFactoryBean();
        factory.setProduct(aaa());
        factory.setDiscount(0.2);
        return factory;
    }

    @Bean
    public DiscountFactoryBean discountFactoryBeanCDRW() {
        DiscountFactoryBean factory = new DiscountFactoryBean();
        factory.setProduct(cdrw());
        factory.setDiscount(0.1);
        return factory;
    }

    @Bean
    public DiscountFactoryBean discountFactoryBeanDVDRW() {
        DiscountFactoryBean factory = new DiscountFactoryBean();
        factory.setProduct(dvdrw());
        factory.setDiscount(0.1);
        return factory;
    }
}
