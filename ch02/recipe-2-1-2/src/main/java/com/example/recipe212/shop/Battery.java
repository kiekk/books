package com.example.recipe212.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Battery extends Product{
    private boolean rechargeable;

    public Battery(String name, double price) {
        super(name, price);
    }
}
