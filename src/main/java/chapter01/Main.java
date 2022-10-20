package chapter01;

public class Main {
    public static void main(String[] args) {
        // 객체 생성 시 외부에서 실 구현체들을 전달
        // 서브 클래스에서는 실제 구현체들을 알 필요가 없습니다.
        Duck mallardDuck = new MallardDuck(new FlyWithWings(), new Quack());
        Duck redHeadDuck = new RedheadDuck(new FlyWithWings(), new Quack());
        Duck rubberDuck = new RubberDuck(new FlyNoWay(), new Squack());

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

    }
}
