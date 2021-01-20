package tacos.web;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import lombok.extern.slf4j.Slf4j;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.Order;
import tacos.Taco;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;

@Slf4j
@Controller
@RequestMapping("/design")
@SessionAttributes("order")
public class DesignTacoController {
	
	private final IngredientRepository ingredientRepo;
	
	private TacoRepository tacoRepo;
	
	@Autowired
	public DesignTacoController(IngredientRepository ingredientRepo, TacoRepository tacoRepo) {
		this.ingredientRepo = ingredientRepo;
		this.tacoRepo = tacoRepo;
	}
	
	@ModelAttribute(name = "order")
	public Order order() {
		return new Order();
	}
	
	@ModelAttribute(name = "taco")
	public Taco taco() {
		return new Taco();
	}
	
	@GetMapping
	public String showDesignForm(Model model) {
		//JdbcTemplat 사용
//		List<Ingredient> ingredients = Arrays.asList(
//			new Ingredient("FLTO", "Flour Tortilla", Type.WRAP),
//			new Ingredient("COTO", "Corn Tortilla", Type.WRAP),
//			new Ingredient("GRBF", "Ground Beef", Type.PROTEIN),
//			new Ingredient("CARN", "Carnitas", Type.PROTEIN),
//			new Ingredient("TMTO", "Diced Tomatoes", Type.VEGGIES),
//			new Ingredient("LETC", "Lettuce", Type.VEGGIES),
//			new Ingredient("CHED", "Cheddar", Type.CHEESE),
//			new Ingredient("JACK", "Monterrey Jack", Type.CHEESE),
//			new Ingredient("SLSA", "Salsa", Type.SAUCE),
//			new Ingredient("SRCR", "Sour Cream", Type.SAUCE)
//		);
		List<Ingredient> ingredients = new ArrayList<>();
		ingredientRepo.findAll().forEach(i -> ingredients.add(i));
		
		Type[] types = Ingredient.Type.values();
		for(Type type : types) {
			model.addAttribute(type.toString().toLowerCase(),
					filterByType(ingredients, type));
		}
		
		model.addAttribute("taco", new Taco());
		
		return "design";
	}
	
	/*
	 * @Valid 어노테이션을 적용해야 실제 VO 객체에 적용했던 유효성 검사들이 실행됩니다.
	 * 이 때 유효성 검사중 어떤 검사라도 에러가 있을 경우 Errors 객체로 전달되며,
	 * Errors 객체에 에러가 있을 경우 design 뷰로 반환합니다.
	 */
	@PostMapping
	public String processDesign(@Valid Taco design, Errors errors, @ModelAttribute Order order) {
		if(errors.hasErrors()) {
			return "design";
		}
		//여기서 타코 디자인(선택한 식자재 내역)을 저장합니다.
		//구현 예정 -> 구현(day02, 2021.01.20)
		//log.info("Processing design: " + design);
		
		Taco saved = tacoRepo.save(design);
		order.addDesign(saved);
		
		return "redirect:/orders/current";
	}
	
	private List<Ingredient> filterByType(
			List<Ingredient> ingredients, Type type){
		return ingredients.stream().filter(x -> x.getType().equals(type))
				.collect(Collectors.toList());
	}
}
