package io.spring.batch.helloworld.repository;

import io.spring.batch.helloworld.domain.Customer;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {
}
