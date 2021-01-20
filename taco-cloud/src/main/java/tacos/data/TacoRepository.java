package tacos.data;

import org.springframework.data.repository.CrudRepository;

import tacos.Taco;

public interface TacoRepository extends CrudRepository<Taco, Long> {
	//CrudRepository 상속으로 아래 메소드는 주석처리
	//Taco save(Taco design);
}
