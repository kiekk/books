package io.spring.batch.helloworld.service;

import io.spring.batch.helloworld.domain.Customer;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    public void logCustomer(Customer customer) {
        System.out.println(customer);
    }

    public void logCustomerAddress(String address,
                                   String city,
                                   String state,
                                   String zip) {
        System.out.printf("I just saved the address:\n%s\n%s, %s\n%s\n",
                address,
                city,
                state,
                zip);
    }
}
