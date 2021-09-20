package jpabook.jpashop.domain;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Data
@Entity
@DiscriminatorValue("B")
public class Book extends Item {
    private String author;
    private String isbn;
}
