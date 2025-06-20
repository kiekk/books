package com.example.recipe212.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Disc extends Product{
    private int capacity;

    public Disc(String name, double price) {
        super(name, price);
    }
}
