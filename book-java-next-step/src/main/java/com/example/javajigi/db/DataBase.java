package com.example.javajigi.db;


import com.example.javajigi.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DataBase {
    // collection 을 사용하여 DB 를 대신한다.
    private static final Map<String, User> users = new HashMap<>();

    public static void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public static User findUserById(String userId) {
        return users.get(userId);
    }

    public static Collection<User> findAll() {
        return users.values();
    }
}