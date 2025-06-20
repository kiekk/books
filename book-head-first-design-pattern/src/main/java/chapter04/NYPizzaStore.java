package chapter04;

public class NYPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String type) {
        Pizza pizza;
        PizzaIngredientFactory ingredientFactory = new NYPizzaIngredientFactory();

        switch (type) {
            case "cheese":
                pizza = new NYStyleCheesePizza(ingredientFactory);
                pizza.setName("뉴욕 스타일 치즈 피자");
                break;
            case "veggie":
                pizza = new NYStyleVeggiePizza(ingredientFactory);
                pizza.setName("뉴욕 스타일 야채 피자");
                break;
            case "clam":
                pizza = new NYStyleClamPizza(ingredientFactory);
                pizza.setName("뉴욕 스타일 조개 피자");
                break;
            case "pepperoni":
                pizza = new NYStylePepperoniPizza(ingredientFactory);
                pizza.setName("뉴욕 스타일 페퍼로니 피자");
                break;
            default:
                return null;
        }
        return pizza;
    }
}
