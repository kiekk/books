package com.example.globalfilterauthorization.controller;

import com.example.globalfilterauthorization.domain.Product;
import com.example.globalfilterauthorization.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/sell")
    public List<Product> sellProduct() {
        List<Product> products = List.of(
                new Product("beer", "nikolai"),
                new Product("candy", "nikolai"),
                new Product("chocolate", "julien"));

        // List.of() 를 사용할 경우 error 발생
        // java.lang.UnsupportedOperationException
        // List.of() 와 같이 컬렉션 팩토리 메소드를 사용할 경우 수정 불가능한 컬렉션이 생성되기 때문에
        // security 에서 컬렉션을 조작, 수정 할 수 없습니다.
        return productService.sellProducts(products);
    }

}
