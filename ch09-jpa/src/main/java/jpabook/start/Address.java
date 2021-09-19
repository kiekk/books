package jpabook.start;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Address {
    @Column(name = "city")  // 매핑할 컬럼 지정 가능
    private String city;
    private String street;
    private String zipcode;
}
