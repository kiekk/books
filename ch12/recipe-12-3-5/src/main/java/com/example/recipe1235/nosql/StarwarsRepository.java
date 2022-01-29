package com.example.recipe1235.nosql;

public interface StarwarsRepository {

    Planet save(Planet planet);
    Character save(Character charachter);
    void printAll();

}
