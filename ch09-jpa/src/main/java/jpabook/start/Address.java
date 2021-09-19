package jpabook.start;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class Address {
    @Column
    private String city;
    private String street;
    private String zipcode;
}
