package chapter04;

public class PizzaStoreDrive {
    public static void main(String[] args) {
        PizzaStore nyStore = new NYPizzaStore();
        PizzaStore chicagoStore = new ChicagoPizzaStore();

        Pizza pizza = nyStore.orderPizza("cheese");
        System.out.println(pizza);

        pizza = chicagoStore.orderPizza("cheese");
        System.out.println(pizza);
    }
}
