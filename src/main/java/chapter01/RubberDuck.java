package chapter01;

public class RubberDuck implements Duck, Quackable {

    @Override
    public void swim() {
        System.out.println("rubber duck swim!!");
    }

    @Override
    public void display() {
        System.out.println("is rubber duck!");
    }

    @Override
    public void quack() {
        System.out.println("BBIBBIBBBIBBIBBI");
    }
}
