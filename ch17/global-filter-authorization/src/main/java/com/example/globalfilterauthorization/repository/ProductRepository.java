package com.example.globalfilterauthorization.repository;


import com.example.globalfilterauthorization.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PostFilter;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @PostFilter("filterObject.owner == authentication.name")
    List<Product> findProductByNameContains(String text);
    
}
