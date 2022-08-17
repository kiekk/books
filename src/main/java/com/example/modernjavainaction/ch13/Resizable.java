package com.example.modernjavainaction.ch13;

public interface Resizable extends Drawable {

    int getWidth();

    int getHeight();

    void setWidth(int width);

    void setHeight(int height);

    void setAbsoluteSize(int width, int height);

    // V2 메소드 추가
    // 이렇게 하면 Ellipse is not abstract and does not override abstract method setRelativeSize(int,int) in Resizable
    // 예외 발생
//    void setRelativeSize(int widthFactor, int heightFactor);

    // 이 경우 디폴트 메소드를 사용하여 해결
    default void setRelativeSize(int widthFactor, int heightFactor) {
        setAbsoluteSize(widthFactor, heightFactor);
    }
}
