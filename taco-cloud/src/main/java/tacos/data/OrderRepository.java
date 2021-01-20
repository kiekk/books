package tacos.data;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import tacos.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {
	//CrudRepository 상속으로 아래 메소드는 주석처리
	//Order save(Order order);
	
	//JPA 메소드 커스터마이징 예시
	/*
	List<Order> findByDeliveryZip(String deliveryZip);
	
	List<Order> readOrdersBydeliveryZipAndPlacedAtBetween(String deliveryZip, Date startDate, Date endDate);
	
	List<Order> findByDeliveryToAnDeliveryCityAllIgnoresCase(String deliveryTo, String deliveryCity);
	
	List<Order> findByDeliveryCityOrderByDeliveryTo(String city);
	
	@Query("Order o where o.deliveryCity='Seattle'")
	List<Order> readOrdersDeliveredInSeattle();
	*/
}
