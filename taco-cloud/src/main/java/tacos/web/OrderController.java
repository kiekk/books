package tacos.web;

import javax.validation.Valid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import lombok.extern.slf4j.Slf4j;
import tacos.Order;
import tacos.User;
import tacos.data.OrderRepository;

@Slf4j
@Controller
@RequestMapping("/orders")
@SessionAttributes("order")
//@ConfigurationProperties(prefix = "taco.orders")
public class OrderController {
	
	private OrderRepository orderRepo;
	
	private OrderProps props;
	
	//pageSize를 OrderProps 클래스로 분리
//	private int pageSize = 20;
//	
//	public void setPageSize(int pageSize) {
//		this.pageSize = pageSize;
//	}
	
	public OrderController(OrderRepository orderRepo, OrderProps props) {
		this.orderRepo = orderRepo;
		this.props = props;
	}
	
	@GetMapping("/current")
	public String orderForm(@AuthenticationPrincipal User user, @ModelAttribute Order order) {
		//model.addAttribute("order", new Order());
		
		if(order.getDeliveryName() == null) {
			order.setDeliveryName(user.getFullname());
		}
		if(order.getDeliveryStreet() == null) {
			order.setDeliveryStreet(user.getStreet());
		}
		if(order.getDeliveryCity() == null) {
			order.setDeliveryCity(user.getCity());
		}
		if(order.getDeliveryState() == null) {
			order.setDeliveryState(user.getState());
		}
		if(order.getDeliveryZip() == null) {
			order.setDeliveryZip(user.getZip());
		}
		return "orderForm";
	}
	
	/*
	 * DesignTacoController의 processDesign() 메소드와 동일한 방식입니다.
	 */
	@PostMapping
	public String processorder(@Valid Order order, Errors errors, SessionStatus sessionStatus, @AuthenticationPrincipal User user) {
		
		if(errors.hasErrors()) {
			return "orderForm";
		}
		
		//log.info("Order submitted: " + order);
		
		order.setUser(user);
		
		orderRepo.save(order);
		sessionStatus.setComplete();
		
		return "redirect:/";
	}
	
	@GetMapping
	public String ordersForUser(@AuthenticationPrincipal User user, Model model) {
		
		//1.페이징 처리, 이 방법은 페이지 수가 정해져있기 때문에 효율적이진 않다.
		//Pageable pageable = PageRequest.of(0, 20);
		
		//2.페이지수를 변수로 설정
		//Pageable pageable = PageRequest.of(0, pageSize);
		
		//3.페이지수를 별도의 클래스로 분리해서 사용
		Pageable pageable = PageRequest.of(0, props.getPageSize());
		
		model.addAttribute("orders", orderRepo.findByUserOrderByPlacesAtDesc(user, pageable));
		
		return "orderList";
	}
}
