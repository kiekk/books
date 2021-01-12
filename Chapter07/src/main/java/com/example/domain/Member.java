package com.example.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.ToString;
import lombok.Setter;

@Getter
@Setter
@ToString
@Entity
public class Member {
	@Id
	private String id;
	private String password;
	private String name;
	@Enumerated(EnumType.STRING)//String으로 설정했기 때문에 권한에 해당하는 값이 문자열로 저장
	private Role role;
	private boolean enabled;
}
