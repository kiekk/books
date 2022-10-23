package chapter05;

public class Singleton {
    private volatile static Singleton uniqueInstance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        // 인스턴스 생성 여부 확인 후 생성되어 있지 않았을 경우에만 동기화
        if (uniqueInstance == null) {
            synchronized (Singleton.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new Singleton();
                }
            }
        }
        return uniqueInstance;
    }

    public String getDescription() {
        return "I'm a classic Singleton!";
    }
}
