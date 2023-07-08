package com.thehecklers.sburrestdemo.repository.coffee;

import com.thehecklers.sburrestdemo.entity.Coffee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeRepository extends JpaRepository<Coffee, String> {
}
