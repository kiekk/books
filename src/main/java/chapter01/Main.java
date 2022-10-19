package chapter01;

public class Main {
    public static void main(String[] args) {
        Duck mallardDuck = new MallardDuck();
        Duck readHeadDuck = new RedheadDuck();

        // 새로운 오리 추가 (고무 오리) rubber duck;
        Duck rubberDuck = new RubberDuck();

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
        readHeadDuck.display();
        rubberDuck.fly(); // 의도하지 않은 결과, 고무 오리는 날 수 없다.

        /*
        각 오리 객체마다의 행동들을 제어하기 위해 Duck 메소드를 매번 오버라이딩 하기 때문에
        정확하게 오리들이 어떤 행동들을 하는지 파악하기 어렵습니다.

        또한 실 구현 객체인 서브 클래스에서 매번 오버라이딩하기 때문에 코드가 중복됩니다.
         */
    }
}
