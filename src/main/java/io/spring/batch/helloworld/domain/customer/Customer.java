package io.spring.batch.helloworld.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Customer {

    private long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String postalCode;
    private String ssn;
    private String emailAddress;
    private String homePhone;
    private String cellPhone;
    private String workPhone;
    private int notificationPreferences;

}
