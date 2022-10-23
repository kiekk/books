package chapter05;

public class Main {
    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();
        System.out.println(singleton.getDescription());

        EnumSingleton enumSingleton = EnumSingleton.UNIQUE_INSTANCE;
        System.out.println(enumSingleton.getDescription());
    }
}
