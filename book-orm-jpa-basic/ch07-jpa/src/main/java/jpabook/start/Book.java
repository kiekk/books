package jpabook.start;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class Book extends Item {
    private String author;
    private String isbn;
}
