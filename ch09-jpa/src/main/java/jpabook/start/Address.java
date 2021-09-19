package jpabook.start;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class Address {
    String street;
    String city;
    String state;
    @Embedded
    Zipcode zipcode;
}
