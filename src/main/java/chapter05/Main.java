package chapter05;

public class Main {
    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();
        System.out.println(singleton.getDescription());

        EnumSingleton enumSingleton = EnumSingleton.UNIQUE_INSTANCE;
        System.out.println(enumSingleton.getDescription());

        ChocolateBoiler boiler = ChocolateBoiler.getInstance();
        boiler.fill();
        boiler.boil();
        boiler.drain();

        ChocolateBoiler boiler2 = ChocolateBoiler.getInstance();
        System.out.println("boiler == boiler2 : " + (boiler == boiler2));
    }
}
