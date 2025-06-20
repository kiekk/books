package jpabook.start;

import javax.persistence.Embeddable;

@Embeddable
public class Zipcode {
    String zip;
    String plusFour;
}
