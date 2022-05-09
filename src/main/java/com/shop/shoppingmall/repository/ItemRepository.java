package com.shop.shoppingmall.repository;

import com.shop.shoppingmall.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByName(String name);

    List<Item> findByNameOrDetail(String name, String detail);

    List<Item> findByPriceLessThan(Integer price);

    List<Item> findByPriceLessThanOrderByPriceDesc(Integer price);

}
