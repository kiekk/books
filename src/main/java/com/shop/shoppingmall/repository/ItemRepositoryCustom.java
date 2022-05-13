package com.shop.shoppingmall.repository;

import com.shop.shoppingmall.dto.ItemSearchDto;
import com.shop.shoppingmall.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {

    Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable);

}
