package com.bookstore.projection;

public class AuthorNameAge {
    private final String name;
    private final int age;

    public AuthorNameAge(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
