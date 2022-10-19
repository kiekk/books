package chapter01;

/*
Rubber Duck 특징 및 단점

1. Rubber Duck 은 장난감이기 때문에 울음소리가 달라야 하므로, quack 메소드를 오버라이딩 해야 합니다.
2. Rubber Duck 은 날 수 없기 때문에 fly 메소드를 오버라이딩 한 후 아무것도 하지 않아야 합니다.
 (하지만 여전히 rubber duck 에서 fly 메소드를 호출 할 수 있습니다.)
 */
public class RubberDuck extends Duck {

    @Override
    public void quack() {
        System.out.println("rubber duck quack!!!");
    }

    @Override
    public void fly() {
        // rubber duck can't flt
        // override and do nothing
    }

    @Override
    public void display() {
        System.out.println("is rubber duck!!");
    }
}
