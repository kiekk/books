package jpabook.start;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "BOARD")
@SecondaryTable(name = "BOARD_DETAIL",
pkJoinColumns = @PrimaryKeyJoinColumn(name = "BOARD_DETAIL_ID"))
public class Board {
    @Id
    @GeneratedValue
    @Column(name = "BOARD_ID")
    private Long id;

    private String title;

    // table을 지정하여 BOARD_DETAIL 테이블에 매핑
    // table을 명시하지 않을 경우 BOARD 테이블에 매핑
    @Column(table = "BOARD_DETAIL")
    private String content;
}
/*
    @SecondaryTable.name: 매핑할 다른 테이블의 이름
    @SEcondaryTable.pkJoinColumns: 매핑할 다른 테이블의 기본 키 컬럼 속성
 */