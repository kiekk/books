package com.example.recipe211.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Sequence {
    private final String id;
    private final String prefix;
    private final String suffix;
}
