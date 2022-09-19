package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;

public class ZipCodeClassifier implements Classifier<Customer, ItemProcessor<Customer, Customer>> {

    private final ItemProcessor<Customer, Customer> oddItemProcessor;
    private final ItemProcessor<Customer, Customer> evenItemProcessor;

    public ZipCodeClassifier(ItemProcessor<Customer, Customer> oddItemProcessor,
                             ItemProcessor<Customer, Customer> evenItemProcessor) {

        this.oddItemProcessor = oddItemProcessor;
        this.evenItemProcessor = evenItemProcessor;
    }

    @Override
    public ItemProcessor<Customer, Customer> classify(Customer classifiable) {
        if (Integer.parseInt(classifiable.getZip()) % 2 == 0) {
            return evenItemProcessor;
        } else {
            return oddItemProcessor;
        }
    }
}
