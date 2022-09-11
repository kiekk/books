package io.spring.batch.helloworld;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
@ToString
public class Transaction {

    private String accountNumber;
    private Date transactionDate;
    private Double amount;

    private final DateFormat formatter =
            new SimpleDateFormat("MM/dd/yyyy");

}
