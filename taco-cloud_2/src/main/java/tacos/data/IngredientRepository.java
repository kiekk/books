package tacos.data;

import org.springframework.data.repository.CrudRepository;

import tacos.Ingredient;

public interface IngredientRepository extends CrudRepository<Ingredient, String> {
	//CrudRepository 상속으로 아래 메소드는 주석처리
//	Iterable<Ingredient> findAll();
//	Ingredient findById(String id);
//	Ingredient save(Ingredient ingredient);
}
