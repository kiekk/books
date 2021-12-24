package com.example.recipe571;

import com.example.recipe571.reactive.court.WebFluxConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.netty.http.server.HttpServer;

@SpringBootApplication
public class Recipe571Application {

    public static void main(String[] args) {
//        SpringApplication.run(Recipe571Application.class, args);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebFluxConfiguration.class);
        HttpHandler handler =
                WebHttpHandlerBuilder.applicationContext(context).build();

        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);

        HttpServer.create().host("localhost").port(8080).handle(adapter).bind().block();
    }

}
