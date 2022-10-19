package chapter01;

public class Main {
    public static void main(String[] args) {
        MallardDuck mallardDuck = new MallardDuck();
        RedheadDuck readHeadDuck = new RedheadDuck();
        RubberDuck rubberDuck = new RubberDuck();

        mallardDuck.quack();
        mallardDuck.swim();
        mallardDuck.display();
        mallardDuck.fly();

        readHeadDuck.quack();
        readHeadDuck.swim();
        readHeadDuck.display();
        readHeadDuck.fly();

        rubberDuck.quack();
        rubberDuck.swim();
        rubberDuck.display();

        /*
        인터페이스를 사용하면 객체 확장에 대한 유연성은 확보할 수 있지만,
        기존의 코드를 전부 바꿔야 하는 문제가 발생합니다.
        또한 서브 믈래스에서 직접 구현하기 때문에 코드를 재사용할 수 없다는 단점도 존재합니다.
         */
    }
}
