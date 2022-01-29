package com.example.recipe1231;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class Recipe1231Application {

    public static void main(String[] args) {
        final String DB_PATH = System.getProperty("user.home") + "/friends";

        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(Paths.get(DB_PATH).toFile());

        Transaction tx1 = db.beginTx();

        Node hello = db.createNode();
        hello.setProperty("msg", "Hello");

        Node world = db.createNode();
        world.setProperty("msg", "World");
        tx1.success();


        db.getAllNodes().stream()
                .map(n -> n.getProperty("msg"))
                .forEach(m -> System.out.println("Msg: " + m));

        db.shutdown();
    }

}
