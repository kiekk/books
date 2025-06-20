package com.example.recipe1236;

import com.example.recipe1236.nosql.Character;
import com.example.recipe1236.nosql.Planet;
import com.example.recipe1236.nosql.StarwarsConfig;
import com.example.recipe1236.nosql.StarwarsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.stream.Stream;

@SpringBootApplication
public class Recipe1236Application {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StarwarsConfig.class);

        StarwarsService service = context.getBean(StarwarsService.class);

        // Planets
        Planet dagobah = new Planet();
        dagobah.setName("Dagobah");

        Planet alderaan = new Planet();
        alderaan.setName("Alderaan");

        Planet tatooine = new Planet();
        tatooine.setName("Tatooine");

        Stream.of(dagobah, alderaan, tatooine).forEach(service::save);

        // Characters
        Character han = new Character();
        han.setName("Han Solo");

        Character leia = new Character();
        leia.setName("Leia Organa");
        leia.setLocation(alderaan);
        leia.addFriend(han);

        Character luke = new Character();
        luke.setName("Luke Skywalker");
        luke.setLocation(tatooine);
        luke.addFriend(han);
        luke.addFriend(leia);

        Character yoda = new Character();
        yoda.setName("Yoda");
        yoda.setLocation(dagobah);
        yoda.setApprentice(luke);

        Stream.of(han, leia, luke, yoda).forEach(service::save);

        service.printAll();

        context.close();
    }

}
