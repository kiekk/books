package com.example;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.controller.BoardController;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class PropertiesTest {
//	
//	@Autowired
//	Environment environment;
//	
//	@Test
//	public void testMethod() {
//		System.out.println("이름 : " + environment.getProperty("author.name"));
//		System.out.println("나이 : " + environment.getProperty("author.age"));
//		System.out.println("국가 : " + environment.getProperty("author.nation"));
//	}
//}

@RunWith(SpringRunner.class)
@SpringBootTest(classes=BoardController.class,
				properties= {"author.name=Apple",
							 "author.age=45",
							 "author.nation=Korea"})
/*
 * @SpringBootTest의 classes에서 지정된 클래스는 컨테이너가 자동으로 메모리에 올립니다.
 * classes에 등록되지 않은 클래스는 객체 생성되지 않기 때문에 불필요한 메모리 낭비를 피할 수 있습니다.
 * 
 * properties 속성을 이용해서 application.properties에 설정된 외부 프로퍼티를 재정의함과 동시에 author.nation이라는 새로운 프로퍼티도 추가할 수 있습니다.
 * 
 */
public class PropertiesTest {
	
	@Autowired
	Environment environment;
	
	@Test
	public void testMethod() {
		System.out.println("이름 : " + environment.getProperty("author.name"));
		System.out.println("나이 : " + environment.getProperty("author.age"));
		System.out.println("국가 : " + environment.getProperty("author.nation"));
	}
}
