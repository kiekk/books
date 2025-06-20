package jpabook.jpashop.domain;

import jpabook.jpashop.enums.DeliveryStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
public class Delivery {
    @Id
    @GeneratedValue
    @Column(name = "DELIVERY_ID")
    private Long id;

    @OneToOne(mappedBy = "delivery")
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;  // Enum [READY(준비), COMP(배송)]

    public Delivery(Address address) {
        this.address = address;
    }
}
