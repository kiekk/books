package tacos.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import tacos.domain.Ingredient;
import tacos.domain.Ingredient.Type;
import tacos.domain.Order;
import tacos.domain.Taco;
import tacos.domain.User;
import tacos.repository.IngredientRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/design")
@RequiredArgsConstructor
@SessionAttributes("order")
public class DesignTacoController {

    private final IngredientRepository ingredientRepository;
    private final TacoRepository tacoRepository;
    private final UserRepository userRepository;


    @ModelAttribute(name = "order")
    public Order order() {
        return new Order();
    }

    @ModelAttribute(name = "taco")
    public Taco taco() {
        return new Taco();
    }

    @GetMapping
    public String showDesignForm(Model model,
                                 Principal principal) {
        List<Ingredient> ingredients = new ArrayList<>();
        ingredientRepository.findAll().forEach(ingredients::add);

        Type[] types = Ingredient.Type.values();
        for (Type type : types) {
            model.addAttribute(type.toString().toLowerCase(),
                    filterByType(ingredients, type));
        }

        String username = principal.getName();
        User user = userRepository.findByUsername(username);
        model.addAttribute("user", user);

        return "design";
    }

    private List<Ingredient> filterByType(
            List<Ingredient> ingredients, Type type) {
        return ingredients
                .stream()
                .filter(x -> x.getType().equals(type))
                .collect(Collectors.toList());
    }

    @PostMapping
    public String processDesign(@Valid Taco design, Errors errors, @ModelAttribute Order order) {
        if (errors.hasErrors()) {
            return "design";
        }

        Taco saved = tacoRepository.save(design);
        order.addDesign(design);

        return "redirect:/orders/current";
    }

}
