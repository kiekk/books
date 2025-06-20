package com.example.globalmethodauthorization.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Employee {

    private String name;
    private List<String> books;
    private List<String> roles;

}
