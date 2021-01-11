package com.example.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity implementation class for Entity: Board
 *
 */
//@Entity
//@Getter
//@Setter
//@ToString
//public class Board {
//	@Id
//	@GeneratedValue
//	private Long seq;
//	private String title;
//	private String writer;
//	private String content;
//	private Date createDate;
//	private Long cnt;
//   
//}

/*
 * 테이블 전략 사용
 */
//@Getter
//@Setter
//@ToString
//@Entity
//@TableGenerator(name="BOARD_SEQ_GENERATOR",
//				table="ALL_SEQUENCES",
//				pkColumnValue="BOARD_SEQ",
//				initialValue=0,
//				allocationSize=1)
//public class Board {
//	@Id
//	@GeneratedValue(strategy=GenerationType.TABLE,
//					generator="BOARD_SEQ_GENERATOR")
//	private Long seq;
//	private String title;
//	private String writer;
//	private String content;
//	@Temporal(TemporalType.DATE)
//	private Date createDate;
//	private Long cnt;
//}

/*
 * 시퀀스 전략 사용
 */
@Getter
@Setter
@ToString
@Entity
@SequenceGenerator(name="BOARD_SEQ_GENERATOR",
					sequenceName="BOARD_SEQUENCE",
					initialValue=1,
					allocationSize=1)
public class Board {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE,
					generator="BOARD_SEQ_GENERATOR")
	private Long seq;
	private String title;
	private String writer;
	private String content;
	@Temporal(TemporalType.DATE)
	private Date createDate;
	private Long cnt;
}
/*
	테이블 전략, 시퀀스 전략 장단점
	테이블 전략 : 데이터베이스에 무관하게 범용적을 사용할 수 있지만, 테이블을 별도로 생성해야 하고 키 값을 자동으로 증가시키기 위해
			 별도의 UPDATE 작업을 수행하는 등 성능의 문제가 있습니다.
	시퀀스 전략 : 별도의 테이블이 필요 없으며 등록 작업의 처리 속도도 빠릅니다. 하지만 오라클이나 H2 데이터베이스와 같이
			 시퀀스를 지원하는 데이터베이스에만 사용할 수 있습니다.
*/