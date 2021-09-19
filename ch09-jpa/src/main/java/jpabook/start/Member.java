package jpabook.start;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    /*
        기본 값 타입: String, age
     */
    private String name;
    private int age;

    @Embedded
    Address homeAddress;


    /*
        같은 임베디드 타입이 존재할 경우
        @AttributeOverrides()를 이용해 매핑 정보 재정의
        @AttributeOverrides()는 엔티티에 설정해야 함
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "COMPANY_CITY")),
            @AttributeOverride(name = "street", column = @Column(name = "COMPANY_STREET")),
            @AttributeOverride(name = "zipcode", column = @Column(name = "COMPANY_ZIPCODE"))
    })
    Address companyAddress;
}
