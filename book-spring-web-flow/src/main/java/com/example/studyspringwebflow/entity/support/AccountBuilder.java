package com.example.studyspringwebflow.entity.support;

import com.example.studyspringwebflow.entity.Account;
import com.example.studyspringwebflow.entity.Address;
import com.example.studyspringwebflow.entity.Permission;
import com.example.studyspringwebflow.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class AccountBuilder extends EntityBuilder<Account> {

    @Override
    void initProduct() {
        this.product = new Account();
    }

    public AccountBuilder credentials(String username, String password) {
        this.product.setUsername(username);
        this.product.setPassword(password);
        return this;
    }

    public AccountBuilder address(String city, String postalCode, String street, String houseNumber, String boxNumber,
                                  String country) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity(city);
        address.setHouseNumber(houseNumber);
        address.setPostalCode(postalCode);
        address.setBoxNumber(boxNumber);
        address.setCountry(country);

        this.product.setAddress(address);
        return this;
    }

    public AccountBuilder roleWithPermissions(Role role, Permission... permissions) {
        this.product.getRoles().add(role);

        for (Permission permission : permissions) {
            role.getPermissions().add(permission);
        }
        return this;
    }

    public AccountBuilder email(String email) {
        this.product.setEmailAddress(email);
        return this;
    }

    public AccountBuilder name(String firstName, String lastName) {
        this.product.setFirstName(firstName);
        this.product.setLastName(lastName);
        return this;
    }

    @Override
    Account assembleProduct() {
        return this.product;
    }
}