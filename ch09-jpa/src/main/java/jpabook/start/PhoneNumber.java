package jpabook.start;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Embeddable
public class PhoneNumber {
    String areaCode;
    String localNumber;

    @ManyToOne
    PhoneServiceProvider provider;
}
