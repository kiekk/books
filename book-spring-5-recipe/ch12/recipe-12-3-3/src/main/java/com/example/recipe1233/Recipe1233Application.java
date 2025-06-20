package com.example.recipe1233;

import com.example.recipe1233.nosql.Character;
import com.example.recipe1233.nosql.Neo4jStarwarsRepository;
import com.example.recipe1233.nosql.Planet;
import com.example.recipe1233.nosql.StarwarsRepository;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootApplication
public class Recipe1233Application {

    public static void main(String[] args) {
        final String DB_PATH = System.getProperty("user.home") + "/starwars";
        final GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(Paths.get(DB_PATH).toFile());

        StarwarsRepository repository = new Neo4jStarwarsRepository(db);

        try (Transaction tx = db.beginTx()) {

            // Planets
            Planet dagobah = new Planet();
            dagobah.setName("Dagobah");

            Planet alderaan = new Planet();
            alderaan.setName("Alderaan");

            Planet tatooine = new Planet();
            tatooine.setName("Tatooine");

            Stream.of(dagobah, alderaan, tatooine).forEach(repository::save);

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

            Stream.of(han, luke, leia, yoda).forEach(repository::save);

            tx.success();
        }

        Result result = db.execute("MATCH (n) RETURN n.name as name");
        result.stream()
                .flatMap(m -> m.entrySet().stream())
                .map(row -> row.getKey() + " : " + row.getValue() + ";")
                .forEach(System.out::println);

        db.shutdown();
    }

}
