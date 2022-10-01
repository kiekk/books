package io.spring.batch.helloworld.batch.classifier;

import io.spring.batch.helloworld.domain.customer.CustomerAddressUpdate;
import io.spring.batch.helloworld.domain.customer.CustomerContactUpdate;
import io.spring.batch.helloworld.domain.customer.CustomerNameUpdate;
import io.spring.batch.helloworld.domain.customer.CustomerUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.classify.Classifier;

@RequiredArgsConstructor
public class CustomerUpdateClassifier implements Classifier<CustomerUpdate, ItemWriter<? super CustomerUpdate>> {

    private final JdbcBatchItemWriter<CustomerUpdate> recordType1ItemWriter;
    private final JdbcBatchItemWriter<CustomerUpdate> recordType2ItemWriter;
    private final JdbcBatchItemWriter<CustomerUpdate> recordType3ItemWriter;

    @Override
    public ItemWriter<? super CustomerUpdate> classify(CustomerUpdate classifiable) {
        if (classifiable instanceof CustomerNameUpdate) {
            return recordType1ItemWriter;
        } else if (classifiable instanceof CustomerAddressUpdate) {
            return recordType2ItemWriter;
        } else if (classifiable instanceof CustomerContactUpdate) {
            return recordType3ItemWriter;
        } else {
            throw new IllegalArgumentException("Invalid type: " + classifiable.getClass().getCanonicalName());
        }
    }
}
