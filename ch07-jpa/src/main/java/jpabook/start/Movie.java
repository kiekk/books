package jpabook.start;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;

@Getter
@Setter
@ToString
@Entity
public class Movie extends Item {
    private String director;
    private String actor;
}
