package chapter01;

public class RubberDuck extends Duck {

    public RubberDuck() {
        this.quackBehavior = new Squack();
        this.flyBehavior = new FlyNoWay();
    }

    @Override
    void swim() {
        System.out.println("rubber duck swim!!");
    }

    @Override
    void display() {
        System.out.println("is rubber duck!!");
    }
}
