package com.example.recipe1235;

import com.example.recipe1235.nosql.Character;
import com.example.recipe1235.nosql.Planet;
import com.example.recipe1235.nosql.StarwarsConfig;
import com.example.recipe1235.nosql.StarwarsRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe1235Application {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StarwarsConfig.class);

        StarwarsRepository repository = context.getBean(StarwarsRepository.class);

        // Planets
        Planet dagobah = new Planet();
        dagobah.setName("Dagobah");

        Planet alderaan = new Planet();
        alderaan.setName("Alderaan");

        Planet tatooine = new Planet();
        tatooine.setName("Tatooine");

        dagobah = repository.save(dagobah);
        repository.save(alderaan);
        repository.save(tatooine);

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

        repository.save(han);
        repository.save(luke);
        repository.save(leia);
        repository.save(yoda);

        repository.printAll();

        context.close();
    }

}
