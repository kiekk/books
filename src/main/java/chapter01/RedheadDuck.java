package chapter01;

public class RedheadDuck implements Duck, Quackable, Flyable {

    @Override
    public void swim() {
        System.out.println("swim!!");
    }

    @Override
    public void display() {
        System.out.println("is red head duck!!");
    }

    @Override
    public void fly() {
        System.out.println("red head duck can fly!!");
    }

    @Override
    public void quack() {
        System.out.println("quack!!");
    }
}
