package chapter05;

// 싱글톤 구현은 가능하지만 enum 이라 객체 확장에 있어 class 보다 좋지 않다.
public enum EnumSingleton {
    UNIQUE_INSTANCE;

    public String getDescription() {
        return "I'm a thread safe Singleton!";
    }
}
