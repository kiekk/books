package com.example.modernjavainaction.ch13;

public class Main {
    public static void main(String[] args) {
        Monster m = new Monster();
        m.rotateBy(180);
        m.moveVertically(10);
        m.setRelativeSize(200, 200);

        Sun s = new Sun();
        s.rotateBy(180);
        s.moveVertically(10);
        // Sun 클래스는 Resizable 인터페이스를 구현하지 않아
        // 크기 조절 불가능능
//       s.setRelativeSize(200, 200);
    }
}
