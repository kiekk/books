package io.spring.batch.helloworld.domain.account;

import io.spring.batch.helloworld.domain.transaction.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Getter
@AllArgsConstructor
public class Account {

    private long id;
    private BigDecimal balance;
    private Date lastStatementDate;
    private final List<Transaction> transactions = new ArrayList<>();

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }
}
