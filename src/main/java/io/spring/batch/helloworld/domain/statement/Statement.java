package io.spring.batch.helloworld.domain.statement;

import io.spring.batch.helloworld.domain.account.Account;
import io.spring.batch.helloworld.domain.customer.Customer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Statement {

    private Customer customer;
    private List<Account> accounts = new ArrayList<>();

    public Statement(Customer customer) {
        this.customer = customer;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts.addAll(accounts);
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

}
