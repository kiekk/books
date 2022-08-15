package com.example.modernjavainaction.ch10.dsl.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = {"type", "stock", "quantity", "price"})
public class Trade {

    public enum Type {
        BUY,
        SELL
    }

    private Type type;
    private Stock stock;
    private int quantity;
    private double price;

    public double getValue() {
        return quantity * price;
    }

}
