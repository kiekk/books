package com.bookstore.dto;

public class AuthorDtoNoSetters {
    private final String name;
    private final int age;

    public AuthorDtoNoSetters(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "AuthorDto{" + "name=" + name + ", age=" + age + '}';
    }
}
