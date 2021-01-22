package tacos.web.api;

import java.util.Date;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import lombok.Getter;
import tacos.Ingredient;
import tacos.Taco;

@Relation(value = "taco", collectionRelation = "tacos")
public class TacoResource extends RepresentationModel<TacoResource> {
	
	private static final IngredientResourceAssembler ingredientAssembler = new IngredientResourceAssembler();

	@Getter
	private final String name;
	
	@Getter
	private final Date createdAt;
	
	@Getter
	private final List<IngredientResource> ingridients;
	
	public TacoResource(Taco taco) {
		this.name = taco.getName();
		this.createdAt = taco.getCreatedAt();
		this.ingridients = (List<IngredientResource>) ingredientAssembler.toCollectionModel(taco.getIngredients());
	}
	
}
