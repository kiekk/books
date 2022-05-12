package com.shop.shoppingmall.service;

import com.shop.shoppingmall.entity.ItemImg;
import com.shop.shoppingmall.repository.ItemImgRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemImgService {

    @Value("${itemImgLocation}")
    private String itemImgLocation;

    private final ItemImgRepository itemImgRepository;
    private final FileService fileService;

    public void saveItemImg(ItemImg itemImg, MultipartFile itemImgFile) throws IOException {
        String originalName = itemImgFile.getOriginalFilename();
        String imageName = "";
        String imageUrl = "";

        // 파일 업로드
        if (!StringUtils.isEmpty(originalName)) {
            imageName = fileService.uploadFile(itemImgLocation, originalName, itemImgFile.getBytes());
            imageUrl = "/images/item" + imageName;
        }

        // 상품 이미지 정보 저장
        itemImg.updateItemImg(originalName, imageName, imageUrl);
        itemImgRepository.save(itemImg);
    }
}
