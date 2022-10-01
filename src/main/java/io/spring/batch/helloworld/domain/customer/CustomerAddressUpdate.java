package io.spring.batch.helloworld.domain.customer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class CustomerAddressUpdate extends CustomerUpdate {

    private String address1;
    private String address2;
    private String city;
    private String state;
    private String postalCode;

    public CustomerAddressUpdate(long customerId, String address1, String address2, String city, String state, String postalCode) {
        super(customerId);
        this.address1 = StringUtils.hasText(address1) ? address1 : null;
        this.address2 = StringUtils.hasText(address2) ? address2 : null;
        this.city = StringUtils.hasText(city) ? city : null;
        this.state = StringUtils.hasText(state) ? state : null;
        this.postalCode = StringUtils.hasText(postalCode) ? postalCode : null;
    }
}
