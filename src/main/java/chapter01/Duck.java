package chapter01;

/*
상속 대신 interface 로 구현하여 확장에 대한 유연성 확보
공통 요소는 Duck 인터페이스에 구현
나머지 특정 기능들은 별도의 인터페이스로 분리 후 실 구현 객체에서
원하는 인터페이스를 구현하도록 설계
 */
public interface Duck {

    void swim();

    void display();

}