package io.spring.batch.helloworld;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Transaction {

    private String accountNumber;
    private Date timeStamp;
    private double amount;

}
