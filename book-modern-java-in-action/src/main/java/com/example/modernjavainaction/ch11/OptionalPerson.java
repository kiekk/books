package com.example.modernjavainaction.ch11;

import java.util.Optional;

public class OptionalPerson {
    private Optional<OptionalCar> car;
    private int age;

    public Optional<OptionalCar> getCar() {
        return car;
    }

    public int getAge() {
        return age;
    }
}
