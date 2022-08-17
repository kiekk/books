package com.example.modernjavainaction.ch13;

public interface Rotatable {

    void setRotatableAngle(int angleInDegrees);

    int getRotatableAngle();

    default void rotateBy(int angleInDegrees) {
        setRotatableAngle((getRotatableAngle() + angleInDegrees) % 360);
    }

}
