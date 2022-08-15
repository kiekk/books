package com.example.modernjavainaction.ch10.dsl.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = {"symbol", "market"})
public class Stock {

    private String symbol;
    private String market;

}
