package chapter01;

public class MallardDuck extends Duck {

    public MallardDuck() {
        this.quackBehavior = new Quack();
        this.flyBehavior = new FlyWithWings();
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
