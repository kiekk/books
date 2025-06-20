package com.example.recipe621.social;

import org.springframework.social.UserIdSource;

public class StaticUserIdSource implements UserIdSource {
    @Override
    public String getUserId() {
        return "anonymous";
    }
}
