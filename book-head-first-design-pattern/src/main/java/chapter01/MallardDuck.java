package chapter01;

public class MallardDuck extends Duck {

    protected MallardDuck(FlyBehavior flyBehavior, QuackBehavior quackBehavior) {
        super(flyBehavior, quackBehavior);
    }

    @Override
    void swim() {
        System.out.println("mallard duck swim!!");
    }

    @Override
    void display() {
        System.out.println("is mallard duck!");
    }
}
