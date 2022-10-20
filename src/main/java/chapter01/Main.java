package chapter01;

public class Main {
    public static void main(String[] args) {
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

        // 노출할 필요가 없는 필드들에 직접 접근할 수 있는 것은 좋지 않습니다. (캡슐화 적용 X)
//        mallardDuck.flyBehavior;
//        mallardDuck.quackBehavior;

//        redHeadDuck.flyBehavior;
//        redHeadDuck.quackBehavior;
        
//        rubberDuck.flyBehavior;
//        rubberDuck.quackBehavior;

    }
}
