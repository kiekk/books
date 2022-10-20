package chapter01;

public class Squack implements QuackBehavior {
    @Override
    public void quack() {
        System.out.println("BBI BBI BBI");
    }
}
