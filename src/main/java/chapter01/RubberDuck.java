package chapter01;

public class RubberDuck extends Duck {

    protected RubberDuck(FlyBehavior flyBehavior, QuackBehavior quackBehavior) {
        super(flyBehavior, quackBehavior);
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
