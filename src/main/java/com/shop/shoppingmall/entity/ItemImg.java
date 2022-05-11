package com.shop.shoppingmall.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "item_img")
@Getter
@Setter
public class ItemImg extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "item_img_id")
    private Long id;

    private String name;

    private String originalName;

    private String url;

    private String repYn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    public void updateItemImg(String originalName, String name, String url) {
        this.originalName = originalName;
        this.name = name;
        this.url = url;
    }
}
