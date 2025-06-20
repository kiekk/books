package jpabook.start;

import lombok.*;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    @Embedded
    private Address homeAddress;

    @ElementCollection
    @CollectionTable(name = "FAVORITE_FOODS",
    joinColumns = @JoinColumn(name = "MEMBER_ID"))
    @Column(name = "FOOD_NAME")
    private Set<String> favoriteFoods = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "ADDRESS",
    joinColumns = @JoinColumn(name = "MEMBER_ID"))
    private List<Address> addressHistory = new ArrayList<>();
    /*
        @CollectionTable을 생략하면 기본 값을 사용해 매핑
        기본 값: {엔티티이름}_{컬렉션 속성 이름}
        ex) 엔티티: Member, 컬렉션 속성 이름: addressHistory
        = Member_addressHistory 테이블에 매핑
     */
}
