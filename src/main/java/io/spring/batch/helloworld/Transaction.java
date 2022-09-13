package io.spring.batch.helloworld;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private Date transactionDate;
    private Double amount;

    private final DateFormat formatter =
            new SimpleDateFormat("MM/dd/yyyy");

}
