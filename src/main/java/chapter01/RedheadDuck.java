package chapter01;

public class RedheadDuck extends Duck {

    public RedheadDuck() {
        this.quackBehavior = new Quack();
        this.flyBehavior = new FlyWithWings();
    }

    @Override
    void swim() {
        System.out.println("red head duck swim!!");
    }

    @Override
    void display() {
        System.out.println("is red head duck!!");
    }
}
