package chapter01;

/*
공통 기능은 Duck 클래스에 추상 메소드로 추출한 후
특정 기능은 해당 기능을 담당하는 별도의 interface 로 분리 후 각 기능별 구현 객체들을 생성
그리고 Duck 은 컴포지트를 사용하여 각 기능들의 상위인 interface 를 필드로 가짐
그리고 해당 interface 에게 기능을 위임
 */
public abstract class Duck {

    FlyBehavior flyBehavior;
    QuackBehavior quackBehavior;

    // QuackBehavior 에게 위임
    void performQuack() {
        quackBehavior.quack();
    }

    // FlyBehavior 에게 위임
    void performFly() {
        flyBehavior.fly();
    }

    abstract void swim();

    abstract void display();

}