package chapter04;

public class ChicagoStyleClamPizza extends Pizza {
    PizzaIngredientFactory ingredientFactory;

    public ChicagoStyleClamPizza(PizzaIngredientFactory ingredientFactory) {
        this.ingredientFactory = ingredientFactory;
    }

    @Override
    void prepare() {
        System.out.println("준비 중: " + name);
        dough = ingredientFactory.createDough();
        sauce = ingredientFactory.createSauce();
        cheese = ingredientFactory.createCheese();
        clam = ingredientFactory.createClam();
    }

    void cut() {
        System.out.println("Cutting the pizza into square slices");
    }
}
