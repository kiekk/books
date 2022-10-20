package chapter01;

public class RedheadDuck extends Duck {

    protected RedheadDuck(FlyBehavior flyBehavior, QuackBehavior quackBehavior) {
        super(flyBehavior, quackBehavior);
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
