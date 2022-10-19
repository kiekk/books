package chapter01;

/*
 모든 오리는 울음소리(Quack) 과 수영(swim) 을 할 수 있기 때문에
 quack, swim 은 공통 메소드로 추출, 그리고 각 오리를 표현할 수 있도록 display 를 추상 메소드로 구현하여
 실 구현 객체에서 표현 방법을 지정
 */
public abstract class Duck {

    public void quack() {
        System.out.println("Quack!!");
    }

    public void swim() {
        System.out.println("Swim!!");
    }

    abstract public void display();
}