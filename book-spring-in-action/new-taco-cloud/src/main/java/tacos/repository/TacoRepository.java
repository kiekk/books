package tacos.repository;

import org.springframework.data.repository.CrudRepository;
import tacos.domain.Taco;

public interface TacoRepository extends CrudRepository<Taco, Long> {
}
