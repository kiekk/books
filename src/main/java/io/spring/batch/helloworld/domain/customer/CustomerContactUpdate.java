package io.spring.batch.helloworld.domain.customer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
public class CustomerContactUpdate extends CustomerUpdate {

    private String emailAddress;
    private String homePhone;
    private String cellPhone;
    private String workPhone;
    private Integer notificationPreference;

    public CustomerContactUpdate(long customerId, String emailAddress, String homePhone, String cellPhone, String workPhone, Integer notificationPreference) {
        super(customerId);
        this.emailAddress = StringUtils.hasText(emailAddress) ? emailAddress : null;
        this.homePhone = StringUtils.hasText(homePhone) ? homePhone : null;
        this.cellPhone = StringUtils.hasText(cellPhone) ? cellPhone : null;
        this.workPhone = StringUtils.hasText(workPhone) ? workPhone : null;
        this.notificationPreference = notificationPreference;
    }
}
