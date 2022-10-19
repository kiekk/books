package chapter01;

/*
 모든 오리는 울음소리(Quack) 과 수영(swim) 을 할 수 있기 때문에
 quack, swim 은 공통 메소드로 추출, 그리고 각 오리를 표현할 수 있도록 display 를 추상 메소드로 구현하여
 실 구현 객체에서 표현 방법을 지정
 */
/*
요구 사항 추가로 인해 오리가 날 수 있도록 fly 메소드를 공통 메소드로 추가
 */
public abstract class Duck {

    public void fly() {
        System.out.println("fly!!!");
    }

    public void quack() {
        System.out.println("Quack!!");
    }

    public void swim() {
        System.out.println("Swim!!");
    }

    abstract public void display();
}