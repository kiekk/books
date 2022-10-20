package chapter01;

public class Main {
    public static void main(String[] args) {
        /*
        각 Duck 구현 객체들을 Duck 타입으로 묶을 수 있습니다.
         */
        Duck mallardDuck = new MallardDuck();
        Duck redHeadDuck = new RedheadDuck();
        Duck rubberDuck = new RubberDuck();

        mallardDuck.swim();
        mallardDuck.display();
        mallardDuck.performQuack();
        mallardDuck.performFly();

        redHeadDuck.swim();
        redHeadDuck.display();
        redHeadDuck.performQuack();
        redHeadDuck.performFly();

        rubberDuck.swim();
        rubberDuck.display();
        rubberDuck.performQuack();
        rubberDuck.performFly();

        /*
        이전에 살펴봤던 상속, 인터페이스의 단점은 해결 되었지만
        각 구현 객체들의 생성자를 보면 QuackBehavior, FlyBehavior 의 실제 구현체들을 지정해주고 있는 것은 단점입니다.
         */
    }
}
