package tacos;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

//WebConfig에서 HomeController를 대체했기 때문에 HomeController.class는 삭제합니다.
//@WebMvcTest(HomeController.class)
@WebMvcTest
public class HomeControllerTest {
	@Autowired
	private MockMvc mockMvc;
	
	@Test
	public void testHomePage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.view().name("home"))
				.andExpect(MockMvcResultMatchers.content().string(
							Matchers.containsString("Welcome to...")));
				
	}
}
