package com.shop.shoppingmall.repository;

import com.shop.shoppingmall.entity.Item;
import com.shop.shoppingmall.enums.ItemSellStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    public void createItemList() {
        for (int i = 0; i < 10; i++) {
            Item item = new Item();
            item.setName("테스트 상품_" + i);
            item.setPrice(10000 + i);
            item.setDetail("테스트 상품 상세 설명_" + i);
            item.setStockNumber(100);
            item.setSellStatus(ItemSellStatus.SELL);
            item.setRegTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            Item savedItem = itemRepository.save(item);
        }
    }

    @Test
    @DisplayName("상품 저장 테스트")
    public void createItemTest() {
        Item item = new Item();
        item.setName("테스트 상품");
        item.setPrice(10000);
        item.setDetail("테스트 상품 상세 설명");
        item.setSellStatus(ItemSellStatus.SELL);
        item.setStockNumber(100);
        item.setRegTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());

        Item savedItem = itemRepository.save(item);

        System.out.println(savedItem);
    }

    @Test
    @DisplayName("상품명 조회 테스트")
    public void findByNameTest() {
        this.createItemList();
        List<Item> itemList = itemRepository.findByName("테스트 상품_1");
        for (Item item : itemList) {
            System.out.println(item);
        }
    }

    @Test
    @DisplayName("상품명, 상품 상세 설명 or 테스트")
    public void findByNameOrDetailTest() {
        this.createItemList();
        List<Item> itemList = itemRepository.findByNameOrDetail("테스트 상품_1", "테스트 상품 상세 설명_5");
        for (Item item : itemList) {
            System.out.println(item);
        }
    }

    @Test
    @DisplayName("가격 LessThan 테스트")
    public void findByPriceLessThanTest() {
        this.createItemList();
        List<Item> itemList = itemRepository.findByPriceLessThan(10005);
        for (Item item : itemList) {
            System.out.println(item);
        }
    }

    @Test
    @DisplayName("가격 내림차순 조회 테스트")
    public void findByPriceLessThanOrderByPriceDescTest() {
        this.createItemList();
        List<Item> itemList = itemRepository.findByPriceLessThanOrderByPriceDesc(10005);
        for (Item item : itemList) {
            System.out.println(item);
        }
    }
}