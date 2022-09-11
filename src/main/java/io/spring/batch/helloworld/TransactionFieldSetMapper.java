package io.spring.batch.helloworld;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class TransactionFieldSetMapper implements FieldSetMapper<Transaction> {

    public Transaction mapFieldSet(FieldSet fieldSet) {
        Transaction transaction = new Transaction();

        transaction.setAccountNumber(fieldSet.readString("accountNumber"));
        transaction.setAmount(fieldSet.readDouble("amount"));
        transaction.setTransactionDate(fieldSet.readDate("transactionDate", "yyyy-MM-dd HH:mm:ss"));

        return transaction;
    }
}
