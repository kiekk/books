package com.example;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.example.service.BoardService;

//@RunWith(SpringRunner.class)
////@WebMvcTest
///*
// * SpringBootTest에는 webEnvironment 속성이 있습니다.
// * 이 속성을 생략하면 기본 값으로 WebEnvironment.MOCK이 설정되어 있는데,
// * 이 설정에 의해서 서블릿 컨테이너가 모킹됩니다.
// */
//@SpringBootTest(webEnvironment=WebEnvironment.MOCK)
///*
// * @SpringBootTest() 설정으로 모킹한 객체를 의존성 주입 받으려면 @AutoConfigureMockMvc를 추가해야 합니다.
// * 이 어노테이션을 사용하면 @WebMvcTest와 비슷하게 사용할 수 있습니다.
// * 다만 @AutoConfigureMockMvc는 컨트롤러뿐만 아니라 @Service나 @Repository가 붙은 객체들을 모두 메모리에 올립니다.
// * 따라서 간단한게 컨트롤러만 테스트하기 위해서는 @WebMvcTest를 사용합니다.
// */
//@AutoConfigureMockMvc
//public class BoardControllerTest {
//	@Autowired
//	private MockMvc mockMvc;
//	
//	//@Test
//	public void testHello() throws Exception {
//		mockMvc.perform(get("/hello").param("name", "apple"))
//		.andExpect(status().isOk())
//		.andExpect(content().string("Hello : apple"))
//		.andDo(print());
//	}
//	
//	/*
//	 * perform() : RequestBuilder 객체를 인자로 받는데, RequestBuilder 객체는 MockMvcRequestBuilders의 정적 메소드를 이용해서 생성합니다.
//	 * MockMvcRequestBuilders의 메소드들은 GET, POST, PUT, DELETE 요청 방식과 매핑되는 get(), post(), put(), delete() 메소드를 제공합니다.
//	 * 
//	 * 이 메소드들은 MockHttpServletRequestBuilder 객체를 반환하는데, 이 객체에 브라우저가 HTTP 요청 ㅍ로토콜에 요청 관련 정보(파라미터,헤더,쿠키 등)을 설정하듯
//	 * 다양한 정보를 설정할 수 있습니다. 
//	 * ex) param() : '키=값'의 파라미터를 여러 개 전달할 수 있습니다.
//	 * 
//	 * perform() 메소드를 이용해서 요청을 전송하면, 그 결과로 ResultActions 객체를 반환하는데, ResultActions는 응답 결과를 검증할 수 있는
//	 * andExpect() 메소드를 제공합니다. andExpect()가 요구하는 ResultMatcher는 MockMvcResultMatchers에 정의된 정적 메소드를 통해 생성할 수 있습니다.
//	 */
//	
//	/*
//	 * 응답 상태 코드 검증
//	 * MockMvcResultMatchers의 status() 메소드는 StatusResultMatchers 객체를 반환하는데 이 객체를 이용하면 응답 상태 코드를 검증할 수 있습니다.
//	 */
//}

//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//public class BoardControllerTest {
//	@Autowired
//	private TestRestTemplate restTemplate;
//	
//	@Test
//	public void testGetBoard() throws Exception {
//		BoardVO board = restTemplate.getForObject("/getBoard", BoardVO.class);
//		System.out.println("getBoard 테스트");
//		assertEquals("테스터", board.getWriter());
//	}
//}


//BoardService 이용하여 테스트
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class BoardControllerTest {
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private BoardService boardService;
	
	@Test
	public void testHello() throws Exception {
		when(boardService.hello("apple")).thenReturn("Hello : apple");
		
		mockMvc.perform(get("/hello").param("name", "apple"))
		.andExpect(status().isOk())
		.andExpect(content().string("Hello : apple"))
		.andDo(print());
	}
}

