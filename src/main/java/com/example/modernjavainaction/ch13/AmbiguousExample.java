package com.example.modernjavainaction.ch13;

public class AmbiguousExample {

    public static void main(String[] args) {
        new C().hello();
        /*
        클래스, 인터페이스로부터 같은 시그니처의 메소드를 상속받을 때의 규칙
        1. 클래스가 항상 우선순위를 갖는다.
        2. 1번 이외의 상황에서는 서브 인터페이스가 우선순위를 갖는다.
        ex) B가 A를 상속받는다면 B가 A를 이긴다.
        3. 디폴트 메소드의 우선순위가 결정되지 않았다면 여러 인터페이스를 상속받는 클래스가 명시적으로
        디폴트 메소드를 오버라이드하고 호출해야 한다.
         */
    }

    static interface A {

        default void hello() {
            System.out.println("Hello from A");
        }

    }

    static interface B {

        default void hello() {
            System.out.println("Hello from B");
        }

    }

    static class C implements B, A {

        @Override
        public void hello() {
            A.super.hello();
        }

    }

}
