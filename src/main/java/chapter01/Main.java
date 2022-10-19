package chapter01;

public class Main {
    public static void main(String[] args) {
        Duck mallardDuck = new MallardDuck();
        Duck readHeadDuck = new RedheadDuck();


        mallardDuck.quack();
        mallardDuck.swim();
        mallardDuck.display();

        readHeadDuck.quack();
        readHeadDuck.swim();
        readHeadDuck.display();
    }
}
