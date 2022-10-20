package chapter04;

public class ChicagoPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String type) {
        Pizza pizza;
        PizzaIngredientFactory ingredientFactory = new NYPizzaIngredientFactory();

        switch (type) {
            case "cheese":
                pizza = new ChicagoStyleCheesePizza(ingredientFactory);
                pizza.setName("시카고 스타일 치즈 피자");
                break;
            case "veggie":
                pizza = new ChicagoStyleVeggiePizza(ingredientFactory);
                pizza.setName("시카고 스타일 야채 피자");
                break;
            case "clam":
                pizza = new ChicagoStyleClamPizza(ingredientFactory);
                pizza.setName("시카고 스타일 조개 피자");
                break;
            case "pepperoni":
                pizza = new ChicagoStylePepperoniPizza(ingredientFactory);
                pizza.setName("시카고 스타일 페퍼로니 피자");
                break;
            default:
                return null;
        }
        return pizza;
    }
}
