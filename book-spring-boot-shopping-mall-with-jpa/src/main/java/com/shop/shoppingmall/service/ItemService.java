package com.shop.shoppingmall.service;

import com.shop.shoppingmall.dto.ItemFormDto;
import com.shop.shoppingmall.dto.ItemImgDto;
import com.shop.shoppingmall.dto.ItemSearchDto;
import com.shop.shoppingmall.dto.MainItemDto;
import com.shop.shoppingmall.entity.Item;
import com.shop.shoppingmall.entity.ItemImg;
import com.shop.shoppingmall.repository.ItemImgRepository;
import com.shop.shoppingmall.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityExistsException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImgService itemImgService;
    private final ItemImgRepository itemImgRepository;

    public Long saveItem(ItemFormDto itemFormDto, List<MultipartFile> itemImgFileList) throws IOException {
        // 상품 등록
        Item item = itemFormDto.createItem();
        itemRepository.save(item);

        // 이미지 등록
        for (int i = 0; i < itemImgFileList.size(); i++) {
            ItemImg itemImg = new ItemImg();
            itemImg.setItem(item);
            String repYn = "N";

            if (i == 0) {
                repYn = "Y";
            }

            itemImg.setRepYn(repYn);
            itemImgService.saveItemImg(itemImg, itemImgFileList.get(i));
        }

        return item.getId();
    }

    @Transactional(readOnly = true)
    public ItemFormDto getItemDtl(Long itemId) {
        List<ItemImg> itemImgList = itemImgRepository.findByItemIdOrderByIdAsc(itemId);

        List<ItemImgDto> itemImgDtoList = new ArrayList<>();

        for (ItemImg itemImg : itemImgList) {
            ItemImgDto itemImgDto = ItemImgDto.of(itemImg);
            itemImgDtoList.add(itemImgDto);
        }

        Item item = itemRepository.findById(itemId).orElseThrow(EntityExistsException::new);

        ItemFormDto itemFormDto = ItemFormDto.of(item);
        itemFormDto.setImgDtoList(itemImgDtoList);
        return itemFormDto;
    }

    public Long updateItem(ItemFormDto itemFormDto,
                           List<MultipartFile> itemImgFileList) throws IOException {
        // 상품 수정
        Item item = itemRepository.findById(itemFormDto.getId()).orElseThrow(EntityExistsException::new);

        item.updateItem(itemFormDto);

        List<Long> itemImgIds = itemFormDto.getImgIds();

        // 이미지 등록
        for (int i = 0; i < itemImgFileList.size(); i++) {
            itemImgService.updateItemImg(itemImgIds.get(i), itemImgFileList.get(i));
        }

        return item.getId();
    }

    @Transactional(readOnly = true)
    public Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable) {
        return itemRepository.getAdminItemPage(itemSearchDto, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MainItemDto> getMainItemPage(ItemSearchDto itemSearchDto,
                                             Pageable pageable) {
        return itemRepository.getMainItemPage(itemSearchDto, pageable);
    }
}
