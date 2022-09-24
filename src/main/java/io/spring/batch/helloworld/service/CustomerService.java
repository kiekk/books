package io.spring.batch.helloworld.service;

import io.spring.batch.helloworld.domain.Customer;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    public void logCustomer(Customer customer) {
        System.out.println(customer);
    }

}
