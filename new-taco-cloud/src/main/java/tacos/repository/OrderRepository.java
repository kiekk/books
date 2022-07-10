package tacos.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import tacos.domain.Order;
import tacos.domain.User;

public interface OrderRepository extends CrudRepository<Order, Long> {
    Object findByUserOrderByPlacedAtDesc(User user, Pageable pageable);
}
