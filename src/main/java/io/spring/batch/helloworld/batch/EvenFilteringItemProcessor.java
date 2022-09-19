package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import org.springframework.batch.item.ItemProcessor;

public class EvenFilteringItemProcessor implements ItemProcessor<Customer, Customer> {

    @Override
    public Customer process(Customer item) {
        return Integer.parseInt(item.getZip()) % 2 == 0 ? null : item;
    }
}
