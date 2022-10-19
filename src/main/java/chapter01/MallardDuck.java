package chapter01;

public class MallardDuck implements Duck, Quackable, Flyable {
    @Override
    public void swim() {
        System.out.println("swim!!");
    }

    @Override
    public void display() {
        System.out.println("is mallard duck!!");
    }

    @Override
    public void fly() {
        System.out.println("mallard duck can fly!");
    }

    @Override
    public void quack() {
        System.out.println("quack!!");
    }
}
