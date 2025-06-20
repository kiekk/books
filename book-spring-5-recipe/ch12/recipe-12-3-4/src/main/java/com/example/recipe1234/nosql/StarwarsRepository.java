package com.example.recipe1234.nosql;

public interface StarwarsRepository {

    Planet save(Planet planet);
    Character save(Character charachter);
    void printAll();

}
